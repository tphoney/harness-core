/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.git;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.git.Constants.GIT_DEFAULT_LOG_PREFIX;
import static io.harness.git.Constants.GIT_HELM_LOG_PREFIX;
import static io.harness.git.Constants.GIT_TERRAFORM_LOG_PREFIX;
import static io.harness.git.Constants.GIT_TRIGGER_LOG_PREFIX;
import static io.harness.git.Constants.GIT_YAML_LOG_PREFIX;
import static io.harness.git.model.GitRepositoryType.HELM;
import static io.harness.git.model.GitRepositoryType.TERRAFORM;
import static io.harness.git.model.GitRepositoryType.TRIGGER;
import static io.harness.git.model.GitRepositoryType.YAML;
import static io.harness.rule.OwnerRule.ABOSII;
import static io.harness.rule.OwnerRule.ARVIND;
import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.HARSH;
import static io.harness.rule.OwnerRule.JAMIE;
import static io.harness.rule.OwnerRule.JELENA;
import static io.harness.rule.OwnerRule.YOGESH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.jgit.diff.DiffEntry.ChangeType.ADD;
import static org.eclipse.jgit.diff.DiffEntry.ChangeType.COPY;
import static org.eclipse.jgit.diff.DiffEntry.ChangeType.DELETE;
import static org.eclipse.jgit.diff.DiffEntry.ChangeType.MODIFY;
import static org.eclipse.jgit.diff.DiffEntry.ChangeType.RENAME;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.GitClientException;
import io.harness.exception.GitConnectionDelegateException;
import io.harness.exception.NonPersistentLockException;
import io.harness.filesystem.FileIo;
import io.harness.git.model.ChangeType;
import io.harness.git.model.GitBaseRequest;
import io.harness.git.model.GitFile;
import io.harness.reflection.ReflectionUtils;
import io.harness.rule.Owner;

import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@Slf4j
@OwnedBy(CI)
public class GitClientHelperTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Inject @InjectMocks GitClientHelper gitClientHelper;

  @Test(expected = GitConnectionDelegateException.class)
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_checkIfGitConnectivityIssueInCaseOfTransportException() {
    gitClientHelper.checkIfGitConnectivityIssue(
        new GitAPIException("Git Exception", new TransportException("Transport Exception")) {});
  }

  @Test(expected = GitConnectionDelegateException.class)
  @Owner(developers = JELENA)
  @Category(UnitTests.class)
  public void test_checkIfGitConnectivityIssueInCaseOfRefNotFoundException() {
    gitClientHelper.checkIfGitConnectivityIssue(new RefNotFoundException("Invalid commit Id"));
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_checkIfGitConnectivityIssueIsNotTrownInCaseOfOtherExceptions() {
    gitClientHelper.checkIfGitConnectivityIssue(new GitAPIException("newTransportException") {});
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void test_checkIfMissingCommitIdIssue() {
    assertThatThrownBy(
        ()
            -> gitClientHelper.checkIfMissingCommitIdIssue(
                new JGitInternalException("Error", new MissingObjectException(ObjectId.zeroId(), "commitId")),
                "commitId"))
        .isInstanceOf(GitClientException.class)
        .hasMessageContaining(
            "Unable to find any references with commit id: commitId. Check provided value for commit id");

    assertThatThrownBy(()
                           -> gitClientHelper.checkIfMissingCommitIdIssue(
                               new MissingObjectException(ObjectId.zeroId(), "commitId"), "commitId"))
        .isInstanceOf(GitClientException.class)
        .hasMessageContaining(
            "Unable to find any references with commit id: commitId. Check provided value for commit id");

    assertThatCode(
        ()
            -> gitClientHelper.checkIfMissingCommitIdIssue(
                new GitAPIException("Git Exception", new TransportException("Transport Exception")) {}, "commitId"))
        .doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testGetRepoDirectory() {
    final String repoDirectory = gitClientHelper.getRepoDirectory(GitBaseRequest.builder()
                                                                      .connectorId("id")
                                                                      .accountId("accountId")
                                                                      .repoType(HELM)
                                                                      .repoUrl("http://github.com/my-repo")
                                                                      .build());
    assertThat(repoDirectory)
        .isEqualTo("./repository/helm/accountId/id/my-repo/9d0502fc8d289f365a3fdcb24607c878b68fad36");
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testGetGitLogMessagePrefix() {
    assertThat(gitClientHelper.getGitLogMessagePrefix(null)).isEqualTo(GIT_DEFAULT_LOG_PREFIX);
    assertThat(gitClientHelper.getGitLogMessagePrefix(TERRAFORM)).isEqualTo(GIT_TERRAFORM_LOG_PREFIX);
    assertThat(gitClientHelper.getGitLogMessagePrefix(YAML)).isEqualTo(GIT_YAML_LOG_PREFIX);
    assertThat(gitClientHelper.getGitLogMessagePrefix(TRIGGER)).isEqualTo(GIT_TRIGGER_LOG_PREFIX);
    assertThat(gitClientHelper.getGitLogMessagePrefix(HELM)).isEqualTo(GIT_HELM_LOG_PREFIX);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testGetLockObject() throws Exception {
    Field cache = ReflectionUtils.getFieldByName(gitClientHelper.getClass(), "cache");
    cache.setAccessible(true);
    LoadingCache<String, Object> cacheMap = (LoadingCache<String, Object>) cache.get(gitClientHelper);
    assertThat(cacheMap.size()).isEqualTo(0L);
    assertThatThrownBy(() -> gitClientHelper.getLockObject(null)).isInstanceOf(NonPersistentLockException.class);
    gitClientHelper.getLockObject("123");
    assertThat(cacheMap.size()).isEqualTo(1L);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testGetFileDownloadRepoDirectory() {
    final String repoDirectory = gitClientHelper.getFileDownloadRepoDirectory(GitBaseRequest.builder()
                                                                                  .connectorId("id")
                                                                                  .accountId("accountId")
                                                                                  .repoType(HELM)
                                                                                  .repoUrl("http://github.com/my-repo")
                                                                                  .build());
    assertThat(repoDirectory)
        .isEqualTo("./repository/gitFileDownloads/accountId/id/my-repo/9d0502fc8d289f365a3fdcb24607c878b68fad36");
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testGetOwnerFromHTTPURL() {
    final String repoName = GitClientHelper.getGitOwner("https://github.com/wings-software/harness-core.git", false);
    assertThat(repoName).isEqualTo("wings-software");
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testGitHubType() {
    final boolean githubSAAS =
        GitClientHelper.isGithubSAAS("https://github.kdc.capitalone.com/wings-software/portal.git");
    assertThat(githubSAAS).isEqualTo(false);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testGetREPOFromHTTPURL() {
    final String repoName = GitClientHelper.getGitRepo("https://github.com/harness/harness-core.git");
    assertThat(repoName).isEqualTo("harness-core");
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testGetOwnerFromSSHURL() {
    final String repoName = GitClientHelper.getGitOwner("git@github.com:wings-software/portal.git", false);
    assertThat(repoName).isEqualTo("wings-software");
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testGetOwnerFromSSHAccountURL() {
    final String repoName = GitClientHelper.getGitOwner("git@github.com:wings-software", true);
    assertThat(repoName).isEqualTo("wings-software");
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testGetOwnerFromAccountURL() {
    final String repoName = GitClientHelper.getGitOwner("https://github.com/wings-software", true);
    assertThat(repoName).isEqualTo("wings-software");
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testGetOwnerFromAccountURLWithSlash() {
    final String repoName = GitClientHelper.getGitOwner("https://github.com/wings-software/", true);
    assertThat(repoName).isEqualTo("wings-software");
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testGetREPOFromSSHURL() {
    final String repoName = GitClientHelper.getGitRepo("git@github.com:wings-software/portal.git");
    assertThat(repoName).isEqualTo("portal");
  }

  @Test
  @Owner(developers = JAMIE)
  @Category(UnitTests.class)
  public void testGetSCMWithoutRepo() {
    final String scmName = GitClientHelper.getGitSCM("ghttps://github.com/");
    assertThat(scmName).isEqualTo("github.com");
  }

  @Test
  @Owner(developers = JAMIE)
  @Category(UnitTests.class)
  public void testGetSCMWithRepo() {
    final String scmName = GitClientHelper.getGitSCM("ghttps://github.com/repo.git");
    assertThat(scmName).isEqualTo("github.com");
  }

  @Test
  @Owner(developers = JAMIE)
  @Category(UnitTests.class)
  public void testGetSCMWithRepoAtEnd() {
    final String scmName = GitClientHelper.getGitSCM("ghttps://github.com@/repo.git");
    assertThat(scmName).isEqualTo("github.com");
  }

  @Test
  @Owner(developers = JAMIE)
  @Category(UnitTests.class)
  public void testGetSCMWithSSHRepo() {
    final String scmName = GitClientHelper.getGitSCM("ssh://git@1.1.1.1:7999/admin/springboot.git");
    assertThat(scmName).isEqualTo("1.1.1.1");
  }

  @Test
  @Owner(developers = JAMIE)
  @Category(UnitTests.class)
  public void testGetSCMWithGITRepo() {
    final String scmName = GitClientHelper.getGitSCM("git@bitbucket.org:foo/bar.git");
    assertThat(scmName).isEqualTo("bitbucket.org");
  }

  @Test
  @Owner(developers = JAMIE)
  @Category(UnitTests.class)
  public void testGetSCMWithGitLabRepo() {
    final String scmName = GitClientHelper.getGitSCM("https://gitlab.com/autouser");
    assertThat(scmName).isEqualTo("gitlab.com");
  }

  @Test
  @Owner(developers = JAMIE)
  @Category(UnitTests.class)
  public void testGetSCMWithGerritRepo() {
    final String scmName = GitClientHelper.getGitSCM("ssh://admin@192.168.1.40:29418/All-Projects");
    assertThat(scmName).isEqualTo("192.168.1.40");
  }

  @Test
  @Owner(developers = JAMIE)
  @Category(UnitTests.class)
  public void testGetSCMWithGerritRepoDoubleAt() {
    final String scmName = GitClientHelper.getGitSCM("ssh://admin@admin.com@192.168.1.40:29418/All-Projects");
    assertThat(scmName).isEqualTo("192.168.1.40");
  }

  @Test
  @Owner(developers = JAMIE)
  @Category(UnitTests.class)
  public void testGetSCMPortWithRepo() {
    final String scmName = GitClientHelper.getGitSCMPort("ghttps://github.com/repo.git");
    assertThat(scmName).isEqualTo(null);
  }

  @Test
  @Owner(developers = JAMIE)
  @Category(UnitTests.class)
  public void testGetSCMPortWithSSHRepo() {
    final String scmName = GitClientHelper.getGitSCMPort("ssh://git@1.1.1.1:7999/admin/springboot.git");
    assertThat(scmName).isEqualTo("7999");
  }

  @Test
  @Owner(developers = JAMIE)
  @Category(UnitTests.class)
  public void testGetSCMPortWithGITRepo() {
    final String scmName = GitClientHelper.getGitSCMPort("git@bitbucket.org:foo/bar.git");
    assertThat(scmName).isEqualTo(null);
  }

  @Test
  @Owner(developers = JAMIE)
  @Category(UnitTests.class)
  public void testGetSCMPortWithGitLabRepo() {
    final String scmName = GitClientHelper.getGitSCMPort("https://gitlab.com/autouser");
    assertThat(scmName).isEqualTo(null);
  }

  @Test
  @Owner(developers = JAMIE)
  @Category(UnitTests.class)
  public void testGetSCMPortWithGerritRepo() {
    final String scmName = GitClientHelper.getGitSCMPort("ssh://admin@192.168.1.40:29418/All-Projects");
    assertThat(scmName).isEqualTo("29418");
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testCreateDirStructureForFileDownload() throws Exception {
    gitClientHelper.createDirStructureForFileDownload(GitBaseRequest.builder()
                                                          .connectorId("id")
                                                          .accountId("accountId")
                                                          .repoType(HELM)
                                                          .repoUrl("http://github.com/my-repo")
                                                          .build());

    assertThat(FileIo.checkIfFileExist(
                   "./repository/gitFileDownloads/accountId/id/my-repo/9d0502fc8d289f365a3fdcb24607c878b68fad36"))
        .isEqualTo(true);
    FileIo.deleteDirectoryAndItsContentIfExists("./repository");
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testAddFiles() throws Exception {
    String repoPath = "./repository/repo/";
    String filePath = "internalPath/1.txt";
    Path repoFilePath = Paths.get(repoPath + filePath);
    FileIo.createDirectoryIfDoesNotExist(repoPath + "/internalPath");
    FileIo.writeFile(repoFilePath, "ABC\nDEF".getBytes());
    Stream<Path> walk = Files.walk(repoFilePath, 1);
    List<GitFile> files = new LinkedList<>();
    walk.forEach(path -> { gitClientHelper.addFiles(files, path, repoPath); });

    assertThat(files.size()).isEqualTo(1);
    assertThat(files.get(0).getFilePath()).isEqualTo(filePath);
    assertThat(files.get(0).getFileContent()).isEqualTo("ABC\nDEF\n");
    FileIo.deleteDirectoryAndItsContentIfExists("./repository");
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testAddFiles_Exception() throws Exception {
    List<GitFile> files = new LinkedList<>();

    assertThatThrownBy(() -> gitClientHelper.addFiles(null, null, null)).isInstanceOf(GitClientException.class);
    assertThatThrownBy(() -> gitClientHelper.addFiles(files, null, null)).isInstanceOf(GitClientException.class);

    String repoPath = "./repository/repo/";
    String filePath = "internalPath/1.txt";
    Path repoFilePath = Paths.get(repoPath + filePath);

    FileIo.createDirectoryIfDoesNotExist(repoPath + "/internalPath");
    FileIo.writeFile(repoFilePath, "ABC\nDEF".getBytes());
    Stream<Path> walk = Files.walk(repoFilePath, 1);
    FileIo.deleteFileIfExists(repoFilePath.toString());
    walk.forEach(path -> {
      assertThatThrownBy(() -> gitClientHelper.addFiles(files, path, repoPath)).isInstanceOf(GitClientException.class);
    });

    assertThat(files.size()).isEqualTo(0);
    FileIo.deleteDirectoryAndItsContentIfExists("./repository");
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testReleaseLock() throws Exception {
    String repoPath = "./repository/repo/";
    FileIo.createDirectoryIfDoesNotExist(repoPath + "/internalPath");
    gitClientHelper.releaseLock(null, repoPath);

    assertThatThrownBy(
        ()
            -> gitClientHelper.releaseLock(
                GitBaseRequest.builder().branch("b1").repoUrl("http://x.y/z").accountId("ACCOUNT_ID").build(), null))
        .isInstanceOf(GitClientException.class);
    FileIo.deleteDirectoryAndItsContentIfExists("./repository");
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testGetChangeType() throws Exception {
    assertThat(gitClientHelper.getChangeType(ADD)).isEqualTo(ChangeType.ADD);
    assertThat(gitClientHelper.getChangeType(MODIFY)).isEqualTo(ChangeType.MODIFY);
    assertThat(gitClientHelper.getChangeType(DELETE)).isEqualTo(ChangeType.DELETE);
    assertThat(gitClientHelper.getChangeType(RENAME)).isEqualTo(ChangeType.RENAME);
    assertThat(gitClientHelper.getChangeType(COPY)).isEqualTo(null);
  }
}
