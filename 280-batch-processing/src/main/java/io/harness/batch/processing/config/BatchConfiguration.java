/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.batch.processing.config;

import static java.nio.charset.StandardCharsets.UTF_8;

import io.harness.manage.ManagedExecutorService;
import io.harness.serializer.YamlUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Slf4j
@Configuration
public class BatchConfiguration {
  @Bean
  public static BatchMainConfig batchMainConfig(Environment environment) throws IOException {
    String configFilePath = environment.getProperty("config-file", "batch-processing-config.yml");
    log.info("batch-processing configFilePath: {}", configFilePath);

    File configFile = new File(configFilePath);
    return new YamlUtils().read(FileUtils.readFileToString(configFile, UTF_8), BatchMainConfig.class);
  }

  @Bean
  public static ThreadPoolTaskScheduler threadPoolTaskScheduler() {
    ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
    threadPoolTaskScheduler.setPoolSize(5);
    return threadPoolTaskScheduler;
  }

  @Bean("accountExecutor")
  @Autowired
  public static Executor accountExecutor(BatchMainConfig batchMainConfig) {
    // for starter, process 2 account in parallel in the beginning, then slowly scale to more
    int parallelism = batchMainConfig.getPodInfoConfig().getAccountParallelism();

    ExecutorService executorService = Executors.newWorkStealingPool(parallelism);

    return new ManagedExecutorService(executorService);
  }
}
