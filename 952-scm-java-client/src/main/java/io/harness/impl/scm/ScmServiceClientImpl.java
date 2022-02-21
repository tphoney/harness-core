/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.impl.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.git.GitClientHelper.isBitBucketSAAS;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FileContentBatchResponse;
import io.harness.beans.gitsync.GitFileDetails;
import io.harness.beans.gitsync.GitFilePathDetails;
import io.harness.beans.gitsync.GitPRCreateRequest;
import io.harness.beans.gitsync.GitWebhookDetails;
import io.harness.constants.Constants;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.ExplanationException;
import io.harness.exception.WingsException;
import io.harness.impl.ScmResponseStatusUtils;
import io.harness.logger.RepoBranchLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.product.ci.scm.proto.Commit;
import io.harness.product.ci.scm.proto.CompareCommitsRequest;
import io.harness.product.ci.scm.proto.CompareCommitsResponse;
import io.harness.product.ci.scm.proto.CreateBranchRequest;
import io.harness.product.ci.scm.proto.CreateBranchResponse;
import io.harness.product.ci.scm.proto.CreateFileResponse;
import io.harness.product.ci.scm.proto.CreatePRRequest;
import io.harness.product.ci.scm.proto.CreatePRResponse;
import io.harness.product.ci.scm.proto.CreateWebhookRequest;
import io.harness.product.ci.scm.proto.CreateWebhookResponse;
import io.harness.product.ci.scm.proto.DeleteFileRequest;
import io.harness.product.ci.scm.proto.DeleteFileResponse;
import io.harness.product.ci.scm.proto.DeleteWebhookRequest;
import io.harness.product.ci.scm.proto.DeleteWebhookResponse;
import io.harness.product.ci.scm.proto.FileBatchContentResponse;
import io.harness.product.ci.scm.proto.FileChange;
import io.harness.product.ci.scm.proto.FileContent;
import io.harness.product.ci.scm.proto.FileModifyRequest;
import io.harness.product.ci.scm.proto.FindCommitRequest;
import io.harness.product.ci.scm.proto.FindCommitResponse;
import io.harness.product.ci.scm.proto.FindFilesInBranchRequest;
import io.harness.product.ci.scm.proto.FindFilesInBranchResponse;
import io.harness.product.ci.scm.proto.FindFilesInCommitRequest;
import io.harness.product.ci.scm.proto.FindFilesInCommitResponse;
import io.harness.product.ci.scm.proto.FindFilesInPRRequest;
import io.harness.product.ci.scm.proto.FindFilesInPRResponse;
import io.harness.product.ci.scm.proto.FindPRRequest;
import io.harness.product.ci.scm.proto.FindPRResponse;
import io.harness.product.ci.scm.proto.GetAuthenticatedUserRequest;
import io.harness.product.ci.scm.proto.GetAuthenticatedUserResponse;
import io.harness.product.ci.scm.proto.GetBatchFileRequest;
import io.harness.product.ci.scm.proto.GetFileRequest;
import io.harness.product.ci.scm.proto.GetLatestCommitOnFileRequest;
import io.harness.product.ci.scm.proto.GetLatestCommitOnFileResponse;
import io.harness.product.ci.scm.proto.GetLatestCommitRequest;
import io.harness.product.ci.scm.proto.GetLatestCommitResponse;
import io.harness.product.ci.scm.proto.GetLatestFileRequest;
import io.harness.product.ci.scm.proto.GetUserReposRequest;
import io.harness.product.ci.scm.proto.GetUserReposResponse;
import io.harness.product.ci.scm.proto.IsLatestFileRequest;
import io.harness.product.ci.scm.proto.IsLatestFileResponse;
import io.harness.product.ci.scm.proto.ListBranchesRequest;
import io.harness.product.ci.scm.proto.ListBranchesResponse;
import io.harness.product.ci.scm.proto.ListCommitsInPRRequest;
import io.harness.product.ci.scm.proto.ListCommitsInPRResponse;
import io.harness.product.ci.scm.proto.ListCommitsRequest;
import io.harness.product.ci.scm.proto.ListCommitsResponse;
import io.harness.product.ci.scm.proto.ListWebhooksRequest;
import io.harness.product.ci.scm.proto.ListWebhooksResponse;
import io.harness.product.ci.scm.proto.PRFile;
import io.harness.product.ci.scm.proto.PageRequest;
import io.harness.product.ci.scm.proto.Provider;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.product.ci.scm.proto.Signature;
import io.harness.product.ci.scm.proto.UpdateFileResponse;
import io.harness.product.ci.scm.proto.WebhookResponse;
import io.harness.service.ScmServiceClient;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@Singleton
@OwnedBy(DX)
public class ScmServiceClientImpl implements ScmServiceClient {
  ScmGitProviderMapper scmGitProviderMapper;
  ScmGitProviderHelper scmGitProviderHelper;

  @Override
  public CreateFileResponse createFile(
      ScmConnector scmConnector, GitFileDetails gitFileDetails, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    FileModifyRequest fileModifyRequest = getFileModifyRequest(scmConnector, gitFileDetails).build();
    CreateFileResponse createFileResponse = scmBlockingStub.createFile(fileModifyRequest);
    if (ScmResponseStatusUtils.isSuccessResponse(createFileResponse.getStatus())
        && isEmpty(createFileResponse.getCommitId())) {
      // In case commit id is empty for any reason, we treat this as an error case even if file got created on git
      return CreateFileResponse.newBuilder()
          .setStatus(Constants.SCM_INTERNAL_SERVER_ERROR_CODE)
          .setError(Constants.SCM_INTERNAL_SERVER_ERROR_MESSAGE)
          .build();
    }
    return createFileResponse;
  }

  private FileModifyRequest.Builder getFileModifyRequest(ScmConnector scmConnector, GitFileDetails gitFileDetails) {
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector, true);
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    return FileModifyRequest.newBuilder()
        .setBranch(gitFileDetails.getBranch())
        .setSlug(slug)
        .setPath(gitFileDetails.getFilePath())
        .setBranch(gitFileDetails.getBranch())
        .setContent(gitFileDetails.getFileContent())
        .setMessage(gitFileDetails.getCommitMessage())
        .setProvider(gitProvider)
        .setSignature(Signature.newBuilder()
                          .setEmail(gitFileDetails.getUserEmail())
                          .setName(gitFileDetails.getUserName())
                          .build());
  }

  @Override
  public UpdateFileResponse updateFile(
      ScmConnector scmConnector, GitFileDetails gitFileDetails, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    Optional<UpdateFileResponse> preChecksStatus =
        runUpdateFileOpsPreChecks(scmConnector, scmBlockingStub, gitFileDetails);
    if (preChecksStatus.isPresent()) {
      return preChecksStatus.get();
    }

    final FileModifyRequest.Builder fileModifyRequestBuilder = getFileModifyRequest(scmConnector, gitFileDetails);
    final FileModifyRequest fileModifyRequest =
        fileModifyRequestBuilder.setBlobId(gitFileDetails.getOldFileSha()).build();
    UpdateFileResponse updateFileResponse = scmBlockingStub.updateFile(fileModifyRequest);

    if (ScmResponseStatusUtils.isSuccessResponse(updateFileResponse.getStatus())
        && isEmpty(updateFileResponse.getCommitId())) {
      // In case commit id is empty for any reason, we treat this as an error case even if file got updated on git
      return UpdateFileResponse.newBuilder()
          .setStatus(Constants.SCM_INTERNAL_SERVER_ERROR_CODE)
          .setError(Constants.SCM_INTERNAL_SERVER_ERROR_MESSAGE)
          .build();
    }
    return updateFileResponse;
  }

  @Override
  public DeleteFileResponse deleteFile(
      ScmConnector scmConnector, GitFileDetails gitFileDetails, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector, true);
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    final DeleteFileRequest deleteFileRequest = DeleteFileRequest.newBuilder()
                                                    .setBranch(gitFileDetails.getBranch())
                                                    .setPath(gitFileDetails.getFilePath())
                                                    .setProvider(gitProvider)
                                                    .setSlug(slug)
                                                    .setBlobId(gitFileDetails.getOldFileSha())
                                                    .setBranch(gitFileDetails.getBranch())
                                                    .setMessage(gitFileDetails.getCommitMessage())
                                                    .setSignature(Signature.newBuilder()
                                                                      .setEmail(gitFileDetails.getUserEmail())
                                                                      .setName(gitFileDetails.getUserName())
                                                                      .build())
                                                    .build();

    return scmBlockingStub.deleteFile(deleteFileRequest);
  }

  @Override
  public FileContent getFileContent(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    final GetFileRequest.Builder gitFileRequestBuilder =
        GetFileRequest.newBuilder().setPath(gitFilePathDetails.getFilePath()).setProvider(gitProvider).setSlug(slug);
    if (gitFilePathDetails.getBranch() != null) {
      gitFileRequestBuilder.setBranch(gitFilePathDetails.getBranch());
    } else if (gitFilePathDetails.getRef() != null) {
      gitFileRequestBuilder.setRef(gitFilePathDetails.getRef());
    }
    return scmBlockingStub.getFile(gitFileRequestBuilder.build());
  }

  private FileBatchContentResponse getContentOfFiles(
      List<String> filePaths, String slug, Provider gitProvider, String ref, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    GetBatchFileRequest batchFileRequest = createBatchFileRequest(filePaths, slug, ref, gitProvider);
    return scmBlockingStub.getBatchFile(batchFileRequest);
  }

  @Override
  public FileContent getLatestFile(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    GetLatestFileRequest getLatestFileRequest = getLatestFileRequestObject(scmConnector, gitFilePathDetails);
    return scmBlockingStub.getLatestFile(getLatestFileRequest);
  }

  @Override
  public IsLatestFileResponse isLatestFile(ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails,
      FileContent fileContent, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    IsLatestFileRequest isLatestFileRequest = getIsLatestFileRequest(scmConnector, gitFilePathDetails, fileContent);
    return scmBlockingStub.isLatestFile(isLatestFileRequest);
  }

  @Override
  public FileContent pushFile(
      ScmConnector scmConnector, GitFileDetails gitFileDetails, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    FileModifyRequest fileModifyRequest = getFileModifyRequest(scmConnector, gitFileDetails).build();
    return scmBlockingStub.pushFile(fileModifyRequest);
  }

  @Override
  public FindFilesInBranchResponse findFilesInBranch(
      ScmConnector scmConnector, String branch, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    FindFilesInBranchRequest findFilesInBranchRequest = getFindFilesInBranchRequest(scmConnector, branch);
    return scmBlockingStub.findFilesInBranch(findFilesInBranchRequest);
  }

  @Override
  public FindFilesInCommitResponse findFilesInCommit(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    FindFilesInCommitRequest findFilesInCommitRequest = getFindFilesInCommitRequest(scmConnector, gitFilePathDetails);
    // still to be resolved
    return scmBlockingStub.findFilesInCommit(findFilesInCommitRequest);
  }

  @Override
  public FindFilesInCommitResponse findFilesInCommit(
      ScmConnector scmConnector, String commitHash, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    FindFilesInCommitRequest findFilesInCommitRequest = getFindFilesInCommitRequest(scmConnector, commitHash);
    return scmBlockingStub.findFilesInCommit(findFilesInCommitRequest);
  }

  @Override
  public FindFilesInPRResponse findFilesInPR(
      ScmConnector scmConnector, int prNumber, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    FindFilesInPRRequest findFilesInPRRequest = getFindFilesInPRRequest(scmConnector, prNumber);
    // still to be resolved
    return scmBlockingStub.findFilesInPR(findFilesInPRRequest);
  }

  @Override
  public GetLatestCommitResponse getLatestCommit(
      ScmConnector scmConnector, String branch, String ref, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    GetLatestCommitRequest getLatestCommitRequest = getLatestCommitRequestObject(scmConnector, branch, ref);
    return scmBlockingStub.getLatestCommit(getLatestCommitRequest);
  }

  @Override
  public ListBranchesResponse listBranches(ScmConnector scmConnector, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    final String slug = scmGitProviderHelper.getSlug(scmConnector);
    final Provider provider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    int pageNumber = 1;
    ListBranchesResponse branchListResponse = null;
    List<String> branchesList = new ArrayList<>();
    do {
      ListBranchesRequest listBranchesRequest = ListBranchesRequest.newBuilder()
                                                    .setSlug(slug)
                                                    .setProvider(provider)
                                                    .setPagination(PageRequest.newBuilder().setPage(pageNumber).build())
                                                    .build();
      branchListResponse = scmBlockingStub.listBranches(listBranchesRequest);
      ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(
          branchListResponse.getStatus(), branchListResponse.getError());
      branchesList.addAll(branchListResponse.getBranchesList());
      pageNumber = branchListResponse.getPagination().getNext();
    } while (hasMoreBranches(branchListResponse));
    return ListBranchesResponse.newBuilder().addAllBranches(branchesList).build();
  }

  private boolean hasMoreBranches(ListBranchesResponse branchList) {
    return branchList != null && branchList.getPagination() != null && branchList.getPagination().getNext() != 0;
  }

  @Override
  public ListCommitsResponse listCommits(
      ScmConnector scmConnector, String branch, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    ListCommitsRequest listCommitsRequest = getListCommitsRequest(scmConnector, branch);
    return scmBlockingStub.listCommits(listCommitsRequest);
  }

  @Override
  public ListCommitsInPRResponse listCommitsInPR(
      ScmConnector scmConnector, long prNumber, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    final String slug = scmGitProviderHelper.getSlug(scmConnector);
    final Provider provider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    int pageNumber = 1;
    if (isBitBucketSAAS(scmConnector.getUrl())) {
      pageNumber = 0;
    }
    ListCommitsInPRResponse commitsInPRResponse;
    List<Commit> commitList = new ArrayList<>();
    do {
      ListCommitsInPRRequest listCommitsInPRRequest =
          ListCommitsInPRRequest.newBuilder()
              .setSlug(slug)
              .setNumber(prNumber)
              .setProvider(provider)
              .setPagination(PageRequest.newBuilder().setPage(pageNumber).build())
              .build();
      commitsInPRResponse = scmBlockingStub.listCommitsInPR(listCommitsInPRRequest);
      commitList.addAll(commitsInPRResponse.getCommitsList());
      pageNumber = commitsInPRResponse.getPagination().getNext();
    } while (pageNumber != 0);
    return ListCommitsInPRResponse.newBuilder().addAllCommits(commitList).build();
  }

  private GetBatchFileRequest createBatchFileRequest(
      List<String> harnessRelatedFilePaths, String slug, String ref, Provider gitProvider) {
    List<GetFileRequest> getBatchFileRequests = new ArrayList<>();
    // todo @deepak: Add the pagination logic to get the list of file content, once scm provides support
    for (String path : emptyIfNull(harnessRelatedFilePaths)) {
      GetFileRequest getFileRequest =
          GetFileRequest.newBuilder().setSlug(slug).setProvider(gitProvider).setRef(ref).setPath(path).build();
      getBatchFileRequests.add(getFileRequest);
    }
    return GetBatchFileRequest.newBuilder().addAllFindRequest(getBatchFileRequests).build();
  }

  private List<String> getPathsOfFilesBelongingToHarness(FindFilesInBranchResponse filesInBranch) {
    List<FileChange> fileList = filesInBranch == null ? Collections.emptyList() : filesInBranch.getFileList();
    if (isEmpty(fileList)) {
      return Collections.emptyList();
    }
    // todo @deepak: Filter and only get files which belongs to harness
    return fileList.stream().map(fileChange -> fileChange.getPath()).collect(toList());
  }

  private GetLatestFileRequest getLatestFileRequestObject(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails) {
    return GetLatestFileRequest.newBuilder()
        .setBranch(gitFilePathDetails.getBranch())
        .setSlug(scmGitProviderHelper.getSlug(scmConnector))
        .setProvider(scmGitProviderMapper.mapToSCMGitProvider(scmConnector))
        .setPath(gitFilePathDetails.getFilePath())
        .build();
  }

  private IsLatestFileRequest getIsLatestFileRequest(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails, FileContent fileContent) {
    return IsLatestFileRequest.newBuilder()
        .setSlug(scmGitProviderHelper.getSlug(scmConnector))
        .setPath(gitFilePathDetails.getFilePath())
        .setBranch(gitFilePathDetails.getBranch())
        .setBlobId(fileContent.getBlobId())
        .setProvider(scmGitProviderMapper.mapToSCMGitProvider(scmConnector))
        .build();
  }

  private FindFilesInBranchRequest getFindFilesInBranchRequest(ScmConnector scmConnector, String branch) {
    return FindFilesInBranchRequest.newBuilder()
        .setSlug(scmGitProviderHelper.getSlug(scmConnector))
        .setBranch(branch)
        .setProvider(scmGitProviderMapper.mapToSCMGitProvider(scmConnector))
        .build();
  }

  private FindFilesInPRRequest getFindFilesInPRRequest(ScmConnector scmConnector, int prNumber) {
    return FindFilesInPRRequest.newBuilder()
        .setSlug(scmGitProviderHelper.getSlug(scmConnector))
        .setNumber(prNumber)
        .setProvider(scmGitProviderMapper.mapToSCMGitProvider(scmConnector))
        .build();
  }

  private FindFilesInCommitRequest getFindFilesInCommitRequest(
      ScmConnector scmConnector, GitFilePathDetails gitFilePathDetails) {
    return FindFilesInCommitRequest.newBuilder()
        .setSlug(scmGitProviderHelper.getSlug(scmConnector))
        .setRef(gitFilePathDetails.getBranch()) // How to get Ref for files????????????
        .setProvider(scmGitProviderMapper.mapToSCMGitProvider(scmConnector))
        .build();
  }

  private FindFilesInCommitRequest getFindFilesInCommitRequest(ScmConnector scmConnector, String commitHash) {
    return FindFilesInCommitRequest.newBuilder()
        .setSlug(scmGitProviderHelper.getSlug(scmConnector))
        .setRef(commitHash)
        .setProvider(scmGitProviderMapper.mapToSCMGitProvider(scmConnector))
        .build();
  }

  private GetLatestCommitRequest getLatestCommitRequestObject(ScmConnector scmConnector, String branch, String ref) {
    final GetLatestCommitRequest.Builder getLatestCommitRequestBuilder =
        GetLatestCommitRequest.newBuilder()
            .setSlug(scmGitProviderHelper.getSlug(scmConnector))
            .setProvider(scmGitProviderMapper.mapToSCMGitProvider(scmConnector));

    if (isNotEmpty(branch)) {
      getLatestCommitRequestBuilder.setBranch(branch);
    } else if (isNotEmpty(ref)) {
      getLatestCommitRequestBuilder.setRef(ref);
    }

    return getLatestCommitRequestBuilder.build();
  }

  private ListBranchesRequest getListBranchesRequest(ScmConnector scmConnector) {
    return ListBranchesRequest.newBuilder()
        .setSlug(scmGitProviderHelper.getSlug(scmConnector))
        .setProvider(scmGitProviderMapper.mapToSCMGitProvider(scmConnector))
        .build();
  }

  private ListCommitsRequest getListCommitsRequest(ScmConnector scmConnector, String branch) {
    return ListCommitsRequest.newBuilder()
        .setSlug(scmGitProviderHelper.getSlug(scmConnector))
        .setBranch(branch)
        .setProvider(scmGitProviderMapper.mapToSCMGitProvider(scmConnector))
        .build();
  }

  private ListCommitsInPRRequest getListCommitsInPRRequest(ScmConnector scmConnector, long prNumber) {
    return ListCommitsInPRRequest.newBuilder()
        .setSlug(scmGitProviderHelper.getSlug(scmConnector))
        .setNumber(prNumber)
        .setProvider(scmGitProviderMapper.mapToSCMGitProvider(scmConnector))
        .build();
  }

  @Override
  public FileContentBatchResponse listFiles(
      ScmConnector connector, Set<String> foldersList, String branchName, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(connector);
    String slug = scmGitProviderHelper.getSlug(connector);
    final GetLatestCommitResponse latestCommitResponse = scmBlockingStub.getLatestCommit(
        GetLatestCommitRequest.newBuilder().setBranch(branchName).setProvider(gitProvider).setSlug(slug).build());
    ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(
        latestCommitResponse.getStatus(), latestCommitResponse.getError());
    String latestCommitId = latestCommitResponse.getCommitId();
    try (AutoLogContext ignore = new RepoBranchLogContext(slug, branchName, latestCommitId, OVERRIDE_ERROR)) {
      List<String> getFilesWhichArePartOfHarness =
          getFileNames(foldersList, slug, gitProvider, branchName, latestCommitId, scmBlockingStub);
      final FileBatchContentResponse contentOfFiles =
          getContentOfFiles(getFilesWhichArePartOfHarness, slug, gitProvider, latestCommitId, scmBlockingStub);
      return FileContentBatchResponse.builder()
          .fileBatchContentResponse(contentOfFiles)
          .commitId(latestCommitId)
          .build();
    }
  }

  @Override
  public FileContentBatchResponse listFoldersFilesByCommitId(
      ScmConnector connector, Set<String> foldersList, String commitId, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(connector);
    String slug = scmGitProviderHelper.getSlug(connector);

    try (AutoLogContext ignore = new RepoBranchLogContext(slug, null, commitId, OVERRIDE_ERROR)) {
      List<String> getFilesWhichArePartOfHarness =
          getFileNames(foldersList, slug, gitProvider, null, commitId, scmBlockingStub);
      final FileBatchContentResponse contentOfFiles =
          getContentOfFiles(getFilesWhichArePartOfHarness, slug, gitProvider, commitId, scmBlockingStub);
      return FileContentBatchResponse.builder().fileBatchContentResponse(contentOfFiles).commitId(commitId).build();
    }
  }

  private List<String> getFileNames(Set<String> foldersList, String slug, Provider gitProvider, String branch,
      String ref, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    GetFilesInFolderForkTask getFilesInFolderTask = GetFilesInFolderForkTask.builder()
                                                        .provider(gitProvider)
                                                        .scmBlockingStub(scmBlockingStub)
                                                        .slug(slug)
                                                        .ref(ref)
                                                        .build();
    List<FileChange> forkJoinTask = getFilesInFolderTask.createForkJoinTask(foldersList);
    return emptyIfNull(forkJoinTask).stream().map(FileChange::getPath).collect(toList());
  }

  // Find content of files for given files paths in the branch at latest commit
  @Override
  public FileContentBatchResponse listFilesByFilePaths(
      ScmConnector connector, List<String> filePaths, String branch, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(connector);
    String slug = scmGitProviderHelper.getSlug(connector);
    final GetLatestCommitResponse latestCommit = scmBlockingStub.getLatestCommit(
        GetLatestCommitRequest.newBuilder().setBranch(branch).setProvider(gitProvider).setSlug(slug).build());
    return processListFilesByFilePaths(connector, filePaths, branch, latestCommit.getCommitId(), scmBlockingStub);
  }

  // Find content of files for given files paths in the branch at given commit
  @Override
  public FileContentBatchResponse listFilesByCommitId(
      ScmConnector connector, List<String> filePaths, String commitId, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    return processListFilesByFilePaths(connector, filePaths, null, commitId, scmBlockingStub);
  }

  @Override
  public CreateBranchResponse createNewBranch(
      ScmConnector scmConnector, String branch, String baseBranchName, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    String latestShaOfBranch = getLatestShaOfBranch(slug, gitProvider, baseBranchName, scmBlockingStub);
    final CreateBranchResponse createBranchResponse =
        createNewBranchFromDefault(slug, gitProvider, branch, latestShaOfBranch, scmBlockingStub);
    try {
      ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(
          createBranchResponse.getStatus(), createBranchResponse.getError());
    } catch (WingsException e) {
      final WingsException cause = ExceptionUtils.cause(ErrorCode.SCM_UNPROCESSABLE_ENTITY, e);
      if (cause != null) {
        throw new ExplanationException(
            String.format("A branch with name %s already exists in the remote Git repository", branch), e);
      } else {
        throw new ExplanationException(String.format("Failed to create branch %s", branch), e);
      }
    }
    return createBranchResponse;
  }

  @Override
  public CreatePRResponse createPullRequest(
      ScmConnector scmConnector, GitPRCreateRequest gitPRCreateRequest, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    CreatePRRequest createPRRequest = CreatePRRequest.newBuilder()
                                          .setSlug(slug)
                                          .setTitle(gitPRCreateRequest.getTitle())
                                          .setProvider(gitProvider)
                                          .setSource(gitPRCreateRequest.getSourceBranch())
                                          .setTarget(gitPRCreateRequest.getTargetBranch())
                                          .build();
    final CreatePRResponse prResponse = scmBlockingStub.createPR(createPRRequest);
    try {
      ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(prResponse.getStatus(), prResponse.getError());
    } catch (WingsException e) {
      final WingsException cause = ExceptionUtils.cause(ErrorCode.SCM_NOT_MODIFIED, e);
      if (cause != null) {
        throw new ExplanationException("A PR already exist for given branches", e);
      } else {
        throw new ExplanationException("Failed to create PR", e);
      }
    }
    return prResponse;
  }

  @Override
  public FindPRResponse findPR(ScmConnector scmConnector, long number, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    FindPRRequest findPRRequest =
        FindPRRequest.newBuilder().setSlug(slug).setNumber(number).setProvider(gitProvider).build();
    final FindPRResponse prResponse = scmBlockingStub.findPR(findPRRequest);
    ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(prResponse.getStatus(), prResponse.getError());
    return prResponse;
  }

  @Override
  public FindCommitResponse findCommit(
      ScmConnector scmConnector, String commitId, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    FindCommitRequest findCommitRequest =
        FindCommitRequest.newBuilder().setSlug(slug).setRef(commitId).setProvider(gitProvider).build();
    final FindCommitResponse commitResponse = scmBlockingStub.findCommit(findCommitRequest);
    ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(
        commitResponse.getStatus(), commitResponse.getError());
    return commitResponse;
  }

  @Override
  public CreateWebhookResponse createWebhook(
      ScmConnector scmConnector, GitWebhookDetails gitWebhookDetails, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    CreateWebhookRequest createWebhookRequest =
        getCreateWebhookRequest(slug, gitProvider, gitWebhookDetails, scmConnector, null);
    return scmBlockingStub.createWebhook(createWebhookRequest);
  }

  public CreateWebhookResponse createWebhook(ScmConnector scmConnector, GitWebhookDetails gitWebhookDetails,
      SCMGrpc.SCMBlockingStub scmBlockingStub, WebhookResponse exisitingWebhook) {
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    CreateWebhookRequest createWebhookRequest =
        getCreateWebhookRequest(slug, gitProvider, gitWebhookDetails, scmConnector, exisitingWebhook);
    return scmBlockingStub.createWebhook(createWebhookRequest);
  }

  private CreateWebhookRequest getCreateWebhookRequest(String slug, Provider gitProvider,
      GitWebhookDetails gitWebhookDetails, ScmConnector scmConnector, WebhookResponse identicalTarget) {
    final CreateWebhookRequest.Builder createWebhookRequestBuilder = CreateWebhookRequest.newBuilder()
                                                                         .setSlug(slug)
                                                                         .setProvider(gitProvider)
                                                                         .setTarget(gitWebhookDetails.getTarget());
    return ScmGitWebhookHelper.getCreateWebhookRequest(
        createWebhookRequestBuilder, gitWebhookDetails, scmConnector, identicalTarget);
  }

  @Override
  public DeleteWebhookResponse deleteWebhook(
      ScmConnector scmConnector, String id, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    DeleteWebhookRequest deleteWebhookRequest = getDeleteWebhookRequest(slug, gitProvider, id);
    return scmBlockingStub.deleteWebhook(deleteWebhookRequest);
  }

  private DeleteWebhookRequest getDeleteWebhookRequest(String slug, Provider gitProvider, String id) {
    return DeleteWebhookRequest.newBuilder().setSlug(slug).setProvider(gitProvider).setId(id).build();
  }

  @Override
  public ListWebhooksResponse listWebhook(ScmConnector scmConnector, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    ListWebhooksRequest listWebhooksRequest = getListWebhookRequest(slug, gitProvider);
    return scmBlockingStub.listWebhooks(listWebhooksRequest);
  }

  @Override
  public CreateWebhookResponse upsertWebhook(
      ScmConnector scmConnector, GitWebhookDetails gitWebhookDetails, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    ListWebhooksResponse listWebhooksResponse = listWebhook(scmConnector, scmBlockingStub);
    final List<WebhookResponse> webhooksList = listWebhooksResponse.getWebhooksList();
    WebhookResponse existingWebhook = null;
    for (WebhookResponse webhookResponse : webhooksList) {
      if (isIdentical(webhookResponse, gitWebhookDetails, scmConnector)) {
        return CreateWebhookResponse.newBuilder().build();
      }
      if (isIdenticalTarget(webhookResponse, gitWebhookDetails)) {
        existingWebhook = webhookResponse;
        final DeleteWebhookResponse deleteWebhookResponse =
            deleteWebhook(scmConnector, webhookResponse.getId(), scmBlockingStub);
        ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(deleteWebhookResponse.getStatus(), null);
      }
    }
    CreateWebhookResponse createWebhookResponse =
        createWebhook(scmConnector, gitWebhookDetails, scmBlockingStub, existingWebhook);
    ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(createWebhookResponse.getStatus(), null);
    return createWebhookResponse;
  }

  @Override
  public CompareCommitsResponse compareCommits(ScmConnector scmConnector, String initialCommitId, String finalCommitId,
      SCMGrpc.SCMBlockingStub scmBlockingStub) {
    List<PRFile> prFiles = new ArrayList<>();
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    CompareCommitsRequest.Builder request = CompareCommitsRequest.newBuilder()
                                                .setSource(initialCommitId)
                                                .setTarget(finalCommitId)
                                                .setSlug(slug)
                                                .setProvider(gitProvider)
                                                .setPagination(PageRequest.newBuilder().setPage(1).build());
    CompareCommitsResponse response;
    // process request in pagination manner
    do {
      response = scmBlockingStub.compareCommits(request.build());
      prFiles.addAll(response.getFilesList());
      // Set next page in the request
      request.setPagination(PageRequest.newBuilder().setPage(response.getPagination().getNext()).build());
    } while (response.getPagination().getNext() != 0);

    return CompareCommitsResponse.newBuilder().addAllFiles(prFiles).build();
  }

  private ListWebhooksRequest getListWebhookRequest(String slug, Provider gitProvider) {
    // Do pagination if webhook goes beyond single page
    return ListWebhooksRequest.newBuilder()
        .setSlug(slug)
        .setProvider(gitProvider)
        .setPagination(PageRequest.newBuilder().setPage(1).build())
        .build();
  }

  @Override
  public GetAuthenticatedUserResponse getAuthenticatedUser(
      ScmConnector scmConnector, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    return scmBlockingStub.getAuthenticatedUser(
        GetAuthenticatedUserRequest.newBuilder().setProvider(gitProvider).build());
  }

  @Override
  public GetUserReposResponse getUserRepos(ScmConnector scmConnector, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector);
    return scmBlockingStub.getUserRepos(GetUserReposRequest.newBuilder().setProvider(gitProvider).build());
  }

  private FileContentBatchResponse processListFilesByFilePaths(ScmConnector connector, List<String> filePaths,
      String branch, String commitId, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(connector);
    String slug = scmGitProviderHelper.getSlug(connector);
    try (AutoLogContext ignore = new RepoBranchLogContext(slug, branch, commitId, OVERRIDE_ERROR)) {
      final FileBatchContentResponse contentOfFiles =
          getContentOfFiles(filePaths, slug, gitProvider, commitId, scmBlockingStub);
      return FileContentBatchResponse.builder().fileBatchContentResponse(contentOfFiles).commitId(commitId).build();
    }
  }

  private CreateBranchResponse createNewBranchFromDefault(String slug, Provider gitProvider, String branch,
      String latestShaOfBranch, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    return scmBlockingStub.createBranch(CreateBranchRequest.newBuilder()
                                            .setName(branch)
                                            .setCommitId(latestShaOfBranch)
                                            .setProvider(gitProvider)
                                            .setSlug(slug)
                                            .build());
  }

  public String getLatestShaOfBranch(
      String slug, Provider gitProvider, String defaultBranchName, SCMGrpc.SCMBlockingStub scmBlockingStub) {
    try {
      GetLatestCommitResponse latestCommit = scmBlockingStub.getLatestCommit(GetLatestCommitRequest.newBuilder()
                                                                                 .setBranch(defaultBranchName)
                                                                                 .setSlug(slug)
                                                                                 .setProvider(gitProvider)
                                                                                 .build());
      ScmResponseStatusUtils.checkScmResponseStatusAndThrowException(latestCommit.getStatus(), latestCommit.getError());
      return latestCommit.getCommit().getSha();
    } catch (Exception ex) {
      log.error(
          "Error encountered while getting latest commit of branch [{}] in slug [{}]", defaultBranchName, slug, ex);
      throw ex;
    }
  }

  private String getGithubToken(Provider gitProvider) {
    return gitProvider.getGithub().getAccessToken();
  }

  private boolean isIdenticalTarget(WebhookResponse webhookResponse, GitWebhookDetails gitWebhookDetails) {
    // Currently we don't add secret however we receive it in response with empty value
    return webhookResponse.getTarget().replace("&secret=", "").equals(gitWebhookDetails.getTarget());
  }

  private boolean isIdentical(
      WebhookResponse webhookResponse, GitWebhookDetails gitWebhookDetails, ScmConnector scmConnector) {
    return isIdenticalTarget(webhookResponse, gitWebhookDetails)
        && ScmGitWebhookHelper.isIdenticalEvents(webhookResponse, gitWebhookDetails.getHookEventType(), scmConnector);
  }

  private GetLatestCommitOnFileResponse getLatestCommitOnFile(
      ScmConnector scmConnector, SCMGrpc.SCMBlockingStub scmBlockingStub, String branch, String filepath) {
    Provider gitProvider = scmGitProviderMapper.mapToSCMGitProvider(scmConnector, true);
    String slug = scmGitProviderHelper.getSlug(scmConnector);
    return scmBlockingStub.getLatestCommitOnFile(GetLatestCommitOnFileRequest.newBuilder()
                                                     .setProvider(gitProvider)
                                                     .setSlug(slug)
                                                     .setBranch(branch)
                                                     .setFilePath(filepath)
                                                     .build());
  }

  private Optional<UpdateFileResponse> runUpdateFileOpsPreChecks(
      ScmConnector scmConnector, SCMGrpc.SCMBlockingStub scmBlockingStub, GitFileDetails gitFileDetails) {
    // Check if current file commit is same as latest commit on file on remote
    if (ConnectorType.BITBUCKET.equals(scmConnector.getConnectorType())) {
      GetLatestCommitOnFileResponse latestCommitResponse = getLatestCommitOnFile(
          scmConnector, scmBlockingStub, gitFileDetails.getBranch(), gitFileDetails.getFilePath());
      if (!latestCommitResponse.getCommitId().equals(gitFileDetails.getCommitId())) {
        return Optional.of(UpdateFileResponse.newBuilder()
                               .setStatus(Constants.SCM_CONFLICT_ERROR_CODE)
                               .setError(Constants.SCM_CONFLICT_ERROR_MESSAGE)
                               .setCommitId(latestCommitResponse.getCommitId())
                               .build());
      }
    }
    return Optional.empty();
  }
}
