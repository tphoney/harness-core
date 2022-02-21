/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.metrics.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.metrics.MetricConstants.ENV_LABEL;
import static io.harness.metrics.MetricConstants.METRIC_LABEL_PREFIX;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.metrics.beans.MetricConfiguration;
import io.harness.metrics.beans.MetricGroup;
import io.harness.metrics.service.api.MetricDefinitionInitializer;
import io.harness.metrics.service.api.MetricService;
import io.harness.serializer.YamlUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import io.opencensus.common.Duration;
import io.opencensus.common.Scope;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsConfiguration;
import io.opencensus.exporter.stats.stackdriver.StackdriverStatsExporter;
import io.opencensus.stats.Measure;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.stats.Stats;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewManager;
import io.opencensus.tags.TagContext;
import io.opencensus.tags.TagContextBuilder;
import io.opencensus.tags.TagKey;
import io.opencensus.tags.TagValue;
import io.opencensus.tags.Tagger;
import io.opencensus.tags.Tags;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;

@Slf4j
@OwnedBy(HarnessTeam.CV)
public class MetricServiceImpl implements MetricService {
  private static final String GOOGLE_APPLICATION_CREDENTIALS = "GOOGLE_APPLICATION_CREDENTIALS";
  private static final String ENV_METRICS_COLLECTION_DISABLED = "METRICS_COLLECTION_DISABLED";
  private static final boolean METRICS_COLLECTION_IS_ENABLED;
  private static final List<MetricConfiguration> METRIC_CONFIG_DEFINITIONS = new ArrayList<>();
  private static final Map<String, MetricGroup> METRIC_GROUP_MAP = new HashMap<>();

  static {
    METRICS_COLLECTION_IS_ENABLED = isMetricPublicationEnabled();
    if (METRICS_COLLECTION_IS_ENABLED) {
      initializeFromYAML();
    }
  }
  private static final Tagger tagger = Tags.getTagger();
  private static final StatsRecorder statsRecorder = Stats.getStatsRecorder();

  private static boolean isMetricPublicationEnabled() {
    String disabled = System.getenv(ENV_METRICS_COLLECTION_DISABLED);
    log.info("METRICS_COLLECTION_DISABLED: {}", disabled);
    // By default, metrics collection is enabled.
    disabled = isEmpty(disabled) ? "false" : disabled.trim().toLowerCase();
    if (disabled.equals("true") || disabled.equals("yes")) {
      return false; // Not enabled.
    }

    String creds = System.getenv(GOOGLE_APPLICATION_CREDENTIALS);
    log.info("GOOGLE_APPLICATION_CREDENTIALS: {}", creds);
    return isNotEmpty(creds);
  }

  private static void recordTaggedStat(Map<TagKey, String> tags, Measure md, Double d) {
    TagContextBuilder contextBuilder = tagger.emptyBuilder();
    tags.forEach((tag, val) -> contextBuilder.put(tag, TagValue.create(val)));
    TagContext tctx = contextBuilder.build();

    try (Scope ss = tagger.withTagContext(tctx)) {
      if (md instanceof MeasureDouble) {
        statsRecorder.newMeasureMap().put((MeasureDouble) md, d).record(tctx);
      } else if (md instanceof MeasureLong) {
        statsRecorder.newMeasureMap().put((MeasureLong) md, (long) (double) d).record(tctx); // TODO: refactor
      }
    }
  }

  private void fetchAndInitMetricDefinitions(List<MetricDefinitionInitializer> metricDefinitionInitializers) {
    List<MetricConfiguration> metricConfigDefinitions = new ArrayList<>();
    metricDefinitionInitializers.forEach(metricDefinitionInitializer -> {
      metricConfigDefinitions.addAll(metricDefinitionInitializer.getMetricConfiguration());
    });
    registerMetricConfigDefinitions(metricConfigDefinitions);
    METRIC_CONFIG_DEFINITIONS.addAll(metricConfigDefinitions);
  }

  private static void initializeFromYAML() {
    List<MetricConfiguration> metricConfigDefinitions = new ArrayList<>();
    long startTime = Instant.now().toEpochMilli();
    Set<String> metricFiles =
        new Reflections("metrics", new ResourcesScanner()).getResources(Pattern.compile(".*\\.yaml"));
    Set<String> metricDefinitionFileNames =
        metricFiles.stream().filter(f -> f.startsWith("metrics/metricDefinitions")).collect(Collectors.toSet());
    metricDefinitionFileNames.forEach(metricDefinition -> {
      try {
        String path = "/" + metricDefinition;
        log.info("Loading metric definitions from {}", path);
        final String yaml = Resources.toString(MetricServiceImpl.class.getResource(path), Charsets.UTF_8);
        YamlUtils yamlUtils = new YamlUtils();
        final MetricConfiguration metricConfiguration =
            yamlUtils.read(yaml, new TypeReference<MetricConfiguration>() {});
        metricConfigDefinitions.add(metricConfiguration);
      } catch (IOException e) {
        throw new IllegalStateException("Error reading metric definition files", e);
      } catch (Exception ex) {
        throw new RuntimeException("Exception occured while reading metric definition files", ex);
      }
    });
    Set<String> metricGroupFileNames =
        metricFiles.stream().filter(f -> f.startsWith("metrics/metricGroups")).collect(Collectors.toSet());
    metricGroupFileNames.forEach(name -> {
      try {
        String path = "/" + name;
        log.info("Loading metrics group definitions from: {}", path);
        final String yaml = Resources.toString(MetricServiceImpl.class.getResource(path), Charsets.UTF_8);
        YamlUtils yamlUtils = new YamlUtils();
        final MetricGroup metricGroup = yamlUtils.read(yaml, new TypeReference<MetricGroup>() {});
        METRIC_GROUP_MAP.put(metricGroup.getIdentifier(), metricGroup);
      } catch (IOException e) {
        throw new IllegalStateException("Error reading metric group file", e);
      }
    });
    registerMetricConfigDefinitions(metricConfigDefinitions);
    METRIC_CONFIG_DEFINITIONS.addAll(metricConfigDefinitions);

    try {
      StackdriverStatsConfiguration configuration =
          StackdriverStatsConfiguration.builder()
              .setExportInterval(Duration.fromMillis(TimeUnit.MINUTES.toMillis(1)))
              .setDeadline(Duration.fromMillis(TimeUnit.MINUTES.toMillis(5)))
              .setConstantLabels(Collections.emptyMap())
              .build();
      StackdriverStatsExporter.createAndRegister(configuration);
      log.info("StackdriverStatsExporter created");
    } catch (Exception ex) {
      log.error("Exception while trying to register stackdriver metrics exporter", ex);
    }
    log.info("Finished loading metrics definitions from YAML. time taken is {} ms, {} metrics loaded",
        Instant.now().toEpochMilli() - startTime, METRIC_CONFIG_DEFINITIONS.size());
    for (MetricConfiguration metricConfiguration : METRIC_CONFIG_DEFINITIONS) {
      log.info("Loaded metric definition: {}", metricConfiguration);
    }
  }

  @Override
  public void initializeMetrics() {
    initializeMetrics(new ArrayList<>());
  }

  @Override
  public void initializeMetrics(List<MetricDefinitionInitializer> metricDefinitionInitializes) {
    if (!METRICS_COLLECTION_IS_ENABLED) {
      return;
    }
    fetchAndInitMetricDefinitions(metricDefinitionInitializes);
  }

  private static void registerMetricConfigDefinitions(List<MetricConfiguration> metricConfigDefinitions) {
    metricConfigDefinitions.forEach(metricConfigDefinition -> {
      List<String> labels = METRIC_GROUP_MAP.get(metricConfigDefinition.getMetricGroup()) == null
          ? new ArrayList<>()
          : METRIC_GROUP_MAP.get(metricConfigDefinition.getMetricGroup()).getLabels();
      if (!labels.contains(ENV_LABEL)) {
        labels.add(ENV_LABEL);
      }
      metricConfigDefinition.getMetrics().forEach(metric -> {
        List<TagKey> tagKeys = new ArrayList<>();
        labels.forEach(label -> tagKeys.add(TagKey.create(label)));

        View view = metric.getView(tagKeys);
        ViewManager vmgr = Stats.getViewManager();
        vmgr.registerView(view);
      });
    });
  }

  @Override
  public void recordMetric(String metricName, double value) {
    try {
      if (!METRICS_COLLECTION_IS_ENABLED) {
        return;
      }
      MetricConfiguration metricConfiguration = null;
      for (MetricConfiguration configDefinition : METRIC_CONFIG_DEFINITIONS) {
        if (configDefinition.getMetrics()
                .stream()
                .map(MetricConfiguration.Metric::getMetricName)
                .collect(Collectors.toList())
                .contains(metricName)) {
          metricConfiguration = configDefinition;
          break;
        }
      }
      if (metricConfiguration == null) {
        throw new IllegalStateException("Unknown metric name while trying to record metrics :" + metricName);
      }
      MetricConfiguration.Metric cvngMetric = metricConfiguration.getMetrics()
                                                  .stream()
                                                  .filter(metric -> metric.getMetricName().equals(metricName))
                                                  .findFirst()
                                                  .get();

      MetricGroup group = METRIC_GROUP_MAP.get(metricConfiguration.getMetricGroup());
      List<String> labelNames =
          group == null || group.getLabels() == null ? Arrays.asList(ENV_LABEL) : group.getLabels();
      Map<String, String> labelVals = group == null ? new HashMap<>() : getLabelValues(labelNames);
      Map<TagKey, String> tagsMap = new HashMap<>();
      for (String labelName : labelNames) {
        if (labelVals.containsKey(labelName)) {
          tagsMap.put(TagKey.create(labelName), labelVals.get(labelName));
        }
      }
      recordTaggedStat(tagsMap, cvngMetric.getMeasure(), value);
    } catch (Exception ex) {
      log.error("Exception occurred while registering a metric", ex);
    }
  }

  @Override
  public void incCounter(String metricName) {
    recordMetric(metricName, 1);
  }

  @Override
  public void recordDuration(String metricName, java.time.Duration duration) {
    recordMetric(metricName, duration.toMillis());
  }

  private Map<String, String> getLabelValues(List<String> labelNames) {
    Map<String, String> context = ThreadContext.getContext();

    String env = System.getenv("ENV");
    if (isEmpty(env)) {
      env = "localhost";
    }

    Map<String, String> labelValues = new HashMap<>();
    labelNames.forEach(label -> {
      String tagKey = METRIC_LABEL_PREFIX + label;
      String tagVal = context.containsKey(tagKey) ? context.get(tagKey) : null;
      if (tagVal != null) {
        labelValues.put(label, tagVal);
      }
    });

    labelValues.put(ENV_LABEL, env);

    // TODO: send a metric for this and add alert on it. If labelNames.size() != labelValues.size()

    return labelValues;
  }
}
