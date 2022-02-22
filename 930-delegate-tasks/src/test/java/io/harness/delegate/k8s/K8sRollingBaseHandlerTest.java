/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.logging.LogLevel.INFO;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ANSHUL;
import static io.harness.rule.OwnerRule.TATHAGAT;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.Kind;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@OwnedBy(CDP)
public class K8sRollingBaseHandlerTest extends CategoryTest {
  @Mock private K8sTaskHelperBase k8sTaskHelperBase;
  @InjectMocks private K8sRollingBaseHandler k8sRollingBaseHandler;

  @Mock private LogCallback logCallback;
  private KubernetesConfig kubernetesConfig = KubernetesConfig.builder().build();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getPodsMultipleWorkloadsInSameNamespace() throws Exception {
    doReturn(Arrays.asList(buildPod(1), buildPod(2)))
        .when(k8sTaskHelperBase)
        .getPodDetails(kubernetesConfig, "default", "release-name", 1);

    List<KubernetesResource> resources = ImmutableList.of(
        KubernetesResource.builder()
            .resourceId(KubernetesResourceId.builder().name("deploy-1").namespace("default").kind("Deployment").build())
            .build(),
        KubernetesResource.builder()
            .resourceId(
                KubernetesResourceId.builder().name("deploy-2").namespace("default").kind("StatefulSet").build())
            .build());

    final List<K8sPod> pods = k8sRollingBaseHandler.getPods(1, resources, kubernetesConfig, "release-name");

    assertThat(pods.stream().map(K8sPod::getName).collect(Collectors.toList())).containsExactly("1", "2");
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getPodsMultipleWorkloadsInMutipleNamespace() throws Exception {
    doReturn(Arrays.asList(buildPod(1), buildPod(2)))
        .when(k8sTaskHelperBase)
        .getPodDetails(kubernetesConfig, "default", "release-name", 1);

    doReturn(Arrays.asList(buildPod(3), buildPod(4), buildPod(5)))
        .when(k8sTaskHelperBase)
        .getPodDetails(kubernetesConfig, "harness", "release-name", 1);

    List<KubernetesResource> resources = ImmutableList.of(
        KubernetesResource.builder()
            .resourceId(KubernetesResourceId.builder().name("deploy-1").namespace("default").kind("Deployment").build())
            .build(),
        KubernetesResource.builder()
            .resourceId(
                KubernetesResourceId.builder().name("deploy-2").namespace("default").kind("StatefulSet").build())
            .build(),
        KubernetesResource.builder()
            .resourceId(
                KubernetesResourceId.builder().name("deploy-3").namespace("harness").kind("StatefulSet").build())
            .build(),
        KubernetesResource.builder()
            .resourceId(KubernetesResourceId.builder().name("deploy-4").namespace("harness").kind("Deployment").build())
            .build());

    final List<K8sPod> pods = k8sRollingBaseHandler.getPods(1, resources, kubernetesConfig, "release-name");

    assertThat(pods.stream().map(K8sPod::getName).collect(Collectors.toList()))
        .containsExactly("1", "2", "3", "4", "5");
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getPodsIfNoWorkloadGiven() throws Exception {
    assertThat(k8sRollingBaseHandler.getPods(1, null, kubernetesConfig, "release-name")).isEmpty();
    assertThat(k8sRollingBaseHandler.getPods(1, Collections.emptyList(), kubernetesConfig, "release-name")).isEmpty();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void testUpdateDeploymentConfigRevision() throws Exception {
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();

    Release.KubernetesResourceIdRevision resourceIdMock = Mockito.mock(Release.KubernetesResourceIdRevision.class);
    Release release = Release.builder().managedWorkloads(asList(resourceIdMock)).build();

    when(k8sTaskHelperBase.getLatestRevision(any(), any(), any())).thenReturn("2");
    when(resourceIdMock.getWorkload())
        .thenReturn(KubernetesResourceId.builder().kind(Kind.DeploymentConfig.name()).build());

    k8sRollingBaseHandler.updateManagedWorkloadsRevision(delegateTaskParams, release, null);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(resourceIdMock).setRevision(captor.capture());
    assertThat(captor.getValue()).isEqualTo("2");

    when(resourceIdMock.getWorkload()).thenReturn(KubernetesResourceId.builder().kind(Kind.Deployment.name()).build());
    verify(resourceIdMock, times(1)).setRevision(anyString());
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testUpdateMultipleWorkloadsRevision() throws Exception {
    K8sDelegateTaskParams delegateTaskParams = K8sDelegateTaskParams.builder().build();

    Release.KubernetesResourceIdRevision deploymentId = Mockito.mock(Release.KubernetesResourceIdRevision.class);
    Release.KubernetesResourceIdRevision statefulSetId = Mockito.mock(Release.KubernetesResourceIdRevision.class);
    Release.KubernetesResourceIdRevision daemonSetId = Mockito.mock(Release.KubernetesResourceIdRevision.class);
    Release release = Release.builder().managedWorkloads(asList(deploymentId, statefulSetId, daemonSetId)).build();

    doReturn("3")
        .when(k8sTaskHelperBase)
        .getLatestRevision(any(Kubectl.class), any(KubernetesResourceId.class), any(K8sDelegateTaskParams.class));

    doReturn(KubernetesResourceId.builder().kind(Kind.Deployment.name()).build()).when(deploymentId).getWorkload();
    doReturn(KubernetesResourceId.builder().kind(Kind.StatefulSet.name()).build()).when(deploymentId).getWorkload();
    doReturn(KubernetesResourceId.builder().kind(Kind.DaemonSet.name()).build()).when(deploymentId).getWorkload();

    k8sRollingBaseHandler.updateManagedWorkloadsRevision(delegateTaskParams, release, null);
    verify(deploymentId, times(1)).setRevision("3");
    verify(statefulSetId, times(1)).setRevision("3");
    verify(daemonSetId, times(1)).setRevision("3");
  }

  private K8sPod buildPod(int name) {
    return K8sPod.builder().name(String.valueOf(name)).build();
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetPods() throws Exception {
    KubernetesResourceId sample = KubernetesResourceId.builder().namespace("default").build();
    List<KubernetesResource> managedWorkload = Arrays.asList(KubernetesResource.builder().resourceId(sample).build(),
        KubernetesResource.builder().resourceId(sample).build());
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().build();

    k8sRollingBaseHandler.getPods(3000L, managedWorkload, kubernetesConfig, "releaseName");

    verify(k8sTaskHelperBase, times(1)).getPodDetails(kubernetesConfig, "default", "releaseName", 3000L);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetExistingPods() throws Exception {
    KubernetesResourceId sample = KubernetesResourceId.builder().namespace("default").build();
    List<KubernetesResource> managedWorkload = Arrays.asList(KubernetesResource.builder().resourceId(sample).build(),
        KubernetesResource.builder().resourceId(sample).build());
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().build();

    k8sRollingBaseHandler.getExistingPods(3000L, managedWorkload, kubernetesConfig, "releaseName", logCallback);

    verify(k8sTaskHelperBase, times(1)).getPodDetails(kubernetesConfig, "default", "releaseName", 3000L);
    verify(logCallback, times(1)).saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testGetExistingPodsFailed() throws Exception {
    KubernetesResourceId sample = KubernetesResourceId.builder().namespace("default").build();
    List<KubernetesResource> managedWorkload = Arrays.asList(KubernetesResource.builder().resourceId(sample).build(),
        KubernetesResource.builder().resourceId(sample).build());
    KubernetesConfig kubernetesConfig = KubernetesConfig.builder().build();
    InvalidRequestException thrownException = new InvalidRequestException("Failed to get pods");

    doThrow(thrownException).when(k8sTaskHelperBase).getPodDetails(kubernetesConfig, "default", "releaseName", 3000L);

    assertThatThrownBy(()
                           -> k8sRollingBaseHandler.getExistingPods(
                               3000L, managedWorkload, kubernetesConfig, "releaseName", logCallback))
        .isEqualTo(thrownException);

    verify(logCallback, times(1)).saveExecutionLog(thrownException.getMessage(), ERROR, FAILURE);
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testAddLabelsInDeploymentSelectorAddSelector() {
    KubernetesResource resource = mock(KubernetesResource.class);
    prepareMockedKubernetesResource(resource);
    List<KubernetesResource> resources = Collections.singletonList(resource);
    k8sRollingBaseHandler.addLabelsInDeploymentSelectorForCanary(true, resources);
    verify(resource, times(1)).addLabelsInDeploymentSelector(anyMap());
  }

  @Test
  @Owner(developers = TATHAGAT)
  @Category(UnitTests.class)
  public void testAddLabelsInDeploymentSelectorNonCanary() {
    KubernetesResource resource = mock(KubernetesResource.class);
    prepareMockedKubernetesResource(resource);
    List<KubernetesResource> resources = Collections.singletonList(resource);

    k8sRollingBaseHandler.addLabelsInDeploymentSelectorForCanary(false, resources);
    verify(resource, never()).addLabelsInDeploymentSelector(anyMap());
  }

  private void prepareMockedKubernetesResource(KubernetesResource resource) {
    KubernetesResourceId resourceId = mock(KubernetesResourceId.class);
    when(resource.getResourceId()).thenReturn(resourceId);
    when(resourceId.getKind()).thenReturn(Kind.Deployment.name());
  }
}
