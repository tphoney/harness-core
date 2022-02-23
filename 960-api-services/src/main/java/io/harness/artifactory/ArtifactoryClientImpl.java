/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.artifactory;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.ARTIFACT_SERVER_ERROR;
import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static io.harness.exception.WingsException.ReportTarget;
import static io.harness.exception.WingsException.USER;
import static io.harness.network.Http.connectableHttpUrl;

import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.jfrog.artifactory.client.ArtifactoryRequest.ContentType.JSON;
import static org.jfrog.artifactory.client.ArtifactoryRequest.ContentType.TEXT;
import static org.jfrog.artifactory.client.ArtifactoryRequest.Method.GET;
import static org.jfrog.artifactory.client.ArtifactoryRequest.Method.POST;

import io.harness.annotations.dev.OwnedBy;
import io.harness.artifact.ArtifactMetadataKeys;
import io.harness.artifact.ArtifactUtilities;
import io.harness.artifacts.beans.BuildDetailsInternal;
import io.harness.artifacts.comparator.BuildDetailsInternalComparatorAscending;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ArtifactoryRegistryException;
import io.harness.exception.ArtifactoryServerException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.network.Http;

import software.wings.common.AlphanumComparator;
import software.wings.helpers.ext.artifactory.FolderPath;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpResponseException;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.ArtifactoryRequest;
import org.jfrog.artifactory.client.ArtifactoryResponse;
import org.jfrog.artifactory.client.ProxyConfig;
import org.jfrog.artifactory.client.impl.ArtifactoryRequestImpl;
import org.jfrog.artifactory.client.model.RepoPath;
import org.jfrog.artifactory.client.model.Repository;
import org.jfrog.artifactory.client.model.impl.PackageTypeImpl;
import org.jfrog.artifactory.client.model.repository.settings.RepositorySettings;

@Slf4j
@OwnedBy(CDC)
public class ArtifactoryClientImpl {
  private static final String REASON = "Reason:";
  private static final String KEY = "key";
  private static final String CREATED_BY = "created_by";
  private static final String SYSTEM = "_system_";
  private static final String RESULTS = "results";
  private static final String DOWNLOAD_FILE_FOR_GENERIC_REPO = "Downloading the file for generic repo";
  private static final String ERROR_OCCURRED_WHILE_RETRIEVING_REPOSITORIES =
      "Error occurred while retrieving repositories";

  public boolean validateArtifactServer(ArtifactoryConfigRequest config) {
    if (!connectableHttpUrl(getBaseUrl(config))) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Check if the Artifactory URL is reachable from your delegate(s)",
          "The given artifactory URL is not reachable",
          new ArtifactoryServerException("Could not reach Artifactory Server at : " + config.getArtifactoryUrl(),
              ErrorCode.INVALID_ARTIFACT_SERVER, USER));
    }
    return isRunning(config);
  }

  public boolean isRunning(ArtifactoryConfigRequest artifactoryConfig) {
    log.info("Validating artifactory server");
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig);
    ArtifactoryRequest repositoryRequest =
        new ArtifactoryRequestImpl().apiUrl("api/repositories/").method(GET).responseType(JSON);
    try {
      ArtifactoryResponse artifactoryResponse = artifactory.restCall(repositoryRequest);
      handleErrorResponse(artifactoryResponse);
      log.info("Validating artifactory server success");
    } catch (RuntimeException e) {
      log.error("Runtime exception occurred while validating artifactory", e);
      handleAndRethrow(e, USER);
    } catch (SocketTimeoutException e) {
      log.error("Exception occurred while validating artifactory", e);
      return true;
    } catch (Exception e) {
      log.error("Exception occurred while validating artifactory", e);
      handleAndRethrow(e, USER);
    }
    return true;
  }

  public static void handleErrorResponse(ArtifactoryResponse artifactoryResponse) throws java.io.IOException {
    if (!artifactoryResponse.isSuccessResponse()) {
      if (artifactoryResponse.getStatusLine().getStatusCode() == 407) {
        throw NestedExceptionUtils.hintWithExplanationException(
            "The proxy settings may be not be configured correctly for Artifactory or the Delegate",
            "The Artifactory Server responded with status code 407",
            new InvalidRequestException(artifactoryResponse.getStatusLine().getReasonPhrase()));
      }
      if (artifactoryResponse.getStatusLine().getStatusCode() == 404) {
        throw NestedExceptionUtils.hintWithExplanationException(
            "Check if the URL is correct. Consider appending `/artifactory` to the connector endpoint if you have not already. Check artifact configuration (repository and image field values).",
            "Artifactory connector URL or artifact configuration may be incorrect or the server is down or the server is not reachable from the delegate",
            new ArtifactoryServerException(
                "Artifactory Server responded with Not Found.", ErrorCode.INVALID_ARTIFACT_SERVER, USER));
      }
      ArtifactoryErrorResponse errorResponse = artifactoryResponse.parseBody(ArtifactoryErrorResponse.class);
      String errorMessage =
          "Request to server failed with status code: " + artifactoryResponse.getStatusLine().getStatusCode();
      if (isNotEmpty(errorResponse.getErrors())) {
        errorMessage +=
            " with message - " + errorResponse.getErrors().stream().map(ArtifactoryError::getMessage).findFirst().get();
      }
      throw NestedExceptionUtils.hintWithExplanationException(
          "The server could have failed authenticate. Please check your credentials", errorMessage,
          new ArtifactoryServerException(
              "Request to server failed with status code: " + artifactoryResponse.getStatusLine().getStatusCode(),
              ErrorCode.INVALID_ARTIFACT_SERVER, USER));
    }
  }

  public static void handleAndRethrow(Exception e, EnumSet<ReportTarget> reportTargets) {
    if (e instanceof HttpResponseException) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "The server could have failed authenticate. Please check your credentials", e.getMessage(),
          new ArtifactoryServerException(e.getMessage(), ErrorCode.INVALID_ARTIFACT_SERVER, reportTargets));
    }
    if (e instanceof SocketTimeoutException) {
      String serverMayNotBeRunningMessaage = e.getMessage() + "."
          + "SocketTimeout: Artifactory server may not be running";
      throw NestedExceptionUtils.hintWithExplanationException(
          "Check if the URL is correct. Consider appending `/artifactory` to the endpoint if you have not already.",
          "Artifactory connector URL may be incorrect or the server may be down or the server may not be reachable from the delegate",
          new ArtifactoryServerException(
              serverMayNotBeRunningMessaage, ErrorCode.INVALID_ARTIFACT_SERVER, reportTargets));
    }
    if (e instanceof WingsException) {
      throw(WingsException) e;
    }
    throw NestedExceptionUtils.hintWithExplanationException(
        "Check if the URL is correct. Consider appending `/artifactory` to the endpoint if you have not already.",
        "Artifactory connector URL may be incorrect or the server may be down or the server may not be reachable from the delegate",
        new ArtifactoryServerException(ExceptionUtils.getMessage(e), ARTIFACT_SERVER_ERROR, reportTargets, e));
  }

  public static Artifactory getArtifactoryClient(ArtifactoryConfigRequest artifactoryConfig) {
    ArtifactoryClientBuilder builder = ArtifactoryClientBuilder.create();
    try {
      builder.setUrl(getBaseUrl(artifactoryConfig));
      if (artifactoryConfig.isHasCredentials()) {
        if (isEmpty(artifactoryConfig.getPassword())) {
          throw NestedExceptionUtils.hintWithExplanationException("Provide a password with username",
              "Password is blank. It is a required field",
              new ArtifactoryServerException(
                  "Password is a required field along with Username", ErrorCode.INVALID_ARTIFACT_SERVER, USER));
        }
        builder.setUsername(artifactoryConfig.getUsername());
        builder.setPassword(new String(artifactoryConfig.getPassword()));
      } else {
        log.info("Username is not set for artifactory config {} . Will use anonymous access.",
            artifactoryConfig.getArtifactoryUrl());
      }

      HttpHost httpProxyHost = Http.getHttpProxyHost(artifactoryConfig.getArtifactoryUrl());
      if (httpProxyHost != null) {
        builder.setProxy(new ProxyConfig(httpProxyHost.getHostName(), httpProxyHost.getPort(), Http.getProxyScheme(),
            Http.getProxyUserName(), Http.getProxyPassword()));
      }
      builder.setSocketTimeout(30000);
      builder.setConnectionTimeout(30000);
    } catch (Exception ex) {
      handleAndRethrow(ex, USER);
    }
    return builder.build();
  }

  public static String getBaseUrl(ArtifactoryConfigRequest artifactoryConfig) {
    return artifactoryConfig.getArtifactoryUrl().endsWith("/") ? artifactoryConfig.getArtifactoryUrl()
                                                               : artifactoryConfig.getArtifactoryUrl() + "/";
  }

  public Map<String, String> getRepositories(
      ArtifactoryConfigRequest artifactoryConfig, List<PackageTypeImpl> packageTypes) {
    log.info("Retrieving repositories for packages {}", packageTypes.toArray());
    Map<String, String> repositories = new HashMap<>();
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig);
    ArtifactoryRequest repositoryRequest =
        new ArtifactoryRequestImpl().apiUrl("api/repositories/").method(GET).responseType(JSON);
    try {
      ArtifactoryResponse response = artifactory.restCall(repositoryRequest);
      handleErrorResponse(response);
      List<Map<Object, Object>> responseList = response.parseBody(List.class);

      for (Map<Object, Object> repository : responseList) {
        String repoKey = repository.get(KEY).toString();
        if (isNotEmpty(packageTypes)) {
          try {
            Repository repo = artifactory.repository(repoKey).get();
            RepositorySettings settings = repo.getRepositorySettings();
            if (packageTypes.contains(settings.getPackageType())) {
              repositories.put(repository.get(KEY).toString(), repository.get(KEY).toString());
            }
          } catch (Exception e) {
            log.warn("Failed to get repository settings for repo {}, Reason {}", repoKey, ExceptionUtils.getMessage(e));
            // TODO : Get Settings api only works for Artifactory Pro
            repositories.put(repository.get(KEY).toString(), repository.get(KEY).toString());
          }
        } else {
          repositories.put(repository.get(KEY).toString(), repository.get(KEY).toString());
        }
      }
      if (repositories.isEmpty()) {
        // Better way of handling Unauthorized access
        log.info("Repositories are not available of package types {} or User not authorized to access artifactory",
            packageTypes);
      }
      log.info("Retrieving repositories for packages {} success", packageTypes.toArray());
    } catch (SocketTimeoutException e) {
      log.error(ERROR_OCCURRED_WHILE_RETRIEVING_REPOSITORIES, e);
      return repositories;
    } catch (Exception e) {
      log.error(ERROR_OCCURRED_WHILE_RETRIEVING_REPOSITORIES, e);
      handleAndRethrow(e, USER);
    }
    return repositories;
  }

  public List<BuildDetails> getBuildDetails(
      ArtifactoryConfigRequest artifactoryConfig, String repositoryName, String artifactPath, int maxVersions) {
    log.info("Retrieving file paths for repositoryName {} artifactPath {}", repositoryName, artifactPath);
    List<String> artifactPaths = new ArrayList<>();
    LinkedHashMap<String, String> map = new LinkedHashMap<>();
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig);
    String artifactName;
    try {
      String aclQuery = "api/search/aql";
      String requestBody;
      if (isNotBlank(artifactPath)) {
        if (artifactPath.charAt(0) == '/') {
          artifactPath = artifactPath.substring(1);
        }
        String subPath;
        if (artifactPath.contains("/")) {
          String[] pathElems = artifactPath.split("/");
          subPath = getPath(Arrays.stream(pathElems).limit(pathElems.length - 1).collect(toList()));
          artifactName = pathElems[pathElems.length - 1];
          if (!artifactName.contains("?") && !artifactName.contains("*")) {
            artifactName = artifactName + "*";
          }
          requestBody = "items.find({\"repo\":\"" + repositoryName + "\"}, {\"path\": {\"$match\":\"" + subPath
              + "\"}}, {\"name\": {\"$match\": \"" + artifactName + "\"}}).sort({\"$desc\" : [\"created\"]}).limit("
              + maxVersions + ")";
        } else {
          artifactPath = artifactPath + "*";
          requestBody = "items.find({\"repo\":\"" + repositoryName + "\"}, {\"depth\": 1}, {\"name\": {\"$match\": \""
              + artifactPath + "\"}}).sort({\"$desc\" : [\"created\"]}).limit(" + maxVersions + ")";
        }
        ArtifactoryRequest repositoryRequest = new ArtifactoryRequestImpl()
                                                   .apiUrl(aclQuery)
                                                   .method(POST)
                                                   .requestBody(requestBody)
                                                   .requestType(TEXT)
                                                   .responseType(JSON);
        ArtifactoryResponse artifactoryResponse = artifactory.restCall(repositoryRequest);
        if (artifactoryResponse.getStatusLine().getStatusCode() == 403
            || artifactoryResponse.getStatusLine().getStatusCode() == 400) {
          log.warn(
              "User not authorized to perform or using OSS version deep level search. Trying with different search api. Message {}",
              artifactoryResponse.getStatusLine().getReasonPhrase());
          return getBuildDetailsForAnonymousUser(
              artifactoryConfig, artifactory, repositoryName, artifactPath, maxVersions);
        }
        Map<String, List> response = artifactoryResponse.parseBody(Map.class);
        if (response != null) {
          List<Map<String, String>> results = response.get(RESULTS);
          if (results != null) {
            for (Map<String, String> result : results) {
              String createdBy = result.get(CREATED_BY);
              if (createdBy == null || !createdBy.equals(SYSTEM)) {
                String path = result.get("path");
                String name = result.get("name");
                String size = String.valueOf(result.get("size"));
                if (path != null && !path.equals(".")) {
                  artifactPaths.add(repositoryName + "/" + path + "/" + name);
                  map.put(repositoryName + "/" + path + "/" + name, size);
                } else {
                  artifactPaths.add(repositoryName + "/" + name);
                  map.put(repositoryName + "/" + name, size);
                }
              }
            }
          }
        }
        log.info("Artifact paths order from Artifactory Server" + artifactPaths);
        Collections.reverse(artifactPaths);
        String finalArtifactPath = artifactPath;
        return artifactPaths.stream()
            .map(path
                -> aBuildDetails()
                       .withNumber(constructBuildNumber(finalArtifactPath, path.substring(path.indexOf('/') + 1)))
                       .withArtifactPath(path)
                       .withBuildUrl(getBaseUrl(artifactoryConfig) + path)
                       .withArtifactFileSize(map.get(path))
                       .withUiDisplayName(
                           "Build# " + constructBuildNumber(finalArtifactPath, path.substring(path.indexOf('/') + 1)))
                       .build())
            .collect(toList());
      } else {
        throw new ArtifactoryServerException("Artifact path can not be empty", INVALID_ARTIFACT_SERVER, USER);
      }
    } catch (Exception e) {
      log.error("Error occurred while retrieving File Paths from Artifactory server {}",
          artifactoryConfig.getArtifactoryUrl(), e);
      handleAndRethrow(e, USER);
    }
    return new ArrayList<>();
  }

  public InputStream downloadArtifacts(ArtifactoryConfigRequest artifactoryConfig, String repoKey,
      Map<String, String> metadata, String artifactPathMetadataKey, String artifactFileNameMetadataKey) {
    Artifactory artifactory = getArtifactoryClient(artifactoryConfig);
    Set<String> artifactNames = new HashSet<>();
    String artifactPath = metadata.get(artifactPathMetadataKey).replaceFirst(repoKey, "").substring(1);
    String artifactName = metadata.get(artifactFileNameMetadataKey);

    try {
      log.info("Artifact name {}", artifactName);
      if (artifactNames.add(artifactName)) {
        if (metadata.get(artifactPathMetadataKey) != null) {
          log.info(DOWNLOAD_FILE_FOR_GENERIC_REPO);
          log.info("Downloading file {} ", artifactPath);
          InputStream inputStream = artifactory.repository(repoKey).download(artifactPath).doDownload();
          log.info("Downloading file {} success", artifactPath);
          return inputStream;
        }
      }
    } catch (Exception e) {
      log.error("Failed to download the artifact of repository {} from path {}", repoKey, artifactPath, e);
      String msg =
          "Failed to download the latest artifacts  of repository [" + repoKey + "] file path [" + artifactPath;
      throw new ArtifactoryServerException(
          msg + REASON + ExceptionUtils.getRootCauseMessage(e), ARTIFACT_SERVER_ERROR, USER);
    }
    log.info(
        "Downloading artifacts from artifactory for repository  {} and file path {} success", repoKey, artifactPath);
    return null;
  }

  private String getPath(List<String> pathElems) {
    StringBuilder groupIdBuilder = new StringBuilder();
    for (int i = 0; i < pathElems.size(); i++) {
      groupIdBuilder.append(pathElems.get(i));
      if (i != pathElems.size() - 1) {
        groupIdBuilder.append('/');
      }
    }
    return groupIdBuilder.toString();
  }

  private String constructBuildNumber(String artifactPattern, String path) {
    String[] tokens = artifactPattern.split("/");
    for (String token : tokens) {
      if (token.contains("*") || token.contains("+")) {
        return path.substring(artifactPattern.indexOf(token));
      }
    }
    return path;
  }

  private List<BuildDetails> getBuildDetailsForAnonymousUser(ArtifactoryConfigRequest artifactoryConfig,
      Artifactory artifactory, String repositoryName, String artifactPath, int maxVersions) {
    List<String> artifactPaths = getFilePathsForAnonymousUser(artifactory, repositoryName, artifactPath, maxVersions);
    return artifactPaths.stream()
        .map(path
            -> aBuildDetails()
                   .withNumber(constructBuildNumber(artifactPath, path.substring(path.indexOf('/') + 1)))
                   .withArtifactPath(path)
                   .withBuildUrl(getBaseUrl(artifactoryConfig) + path)
                   .withUiDisplayName(
                       "Build# " + constructBuildNumber(artifactPath, path.substring(path.indexOf('/') + 1)))
                   .build())
        .collect(toList());
  }

  private List<String> getFilePathsForAnonymousUser(
      Artifactory artifactory, String repoKey, String artifactPath, int maxVersions) {
    log.info("Retrieving file paths for repoKey {} artifactPath {}", repoKey, artifactPath);
    List<String> artifactPaths = new ArrayList<>();

    List<software.wings.helpers.ext.artifactory.FolderPath> folderPaths;
    try {
      if (isNotBlank(artifactPath)) {
        if (artifactPath.charAt(0) == '/') {
          artifactPath = artifactPath.substring(1);
        }
        if (artifactPath.endsWith("/")) {
          artifactPath = artifactPath.substring(0, artifactPath.length() - 1);
        }
        Pattern pattern = Pattern.compile(artifactPath.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));
        if (artifactPath.contains("/")) {
          String[] pathElems = artifactPath.split("/");
          String subPath = getPath(Arrays.stream(pathElems).limit(pathElems.length - 1).collect(toList()));
          if (!subPath.contains("?") && !subPath.contains("*")) {
            folderPaths = getFolderPaths(artifactory, repoKey, "/" + subPath);
            if (folderPaths != null) {
              for (software.wings.helpers.ext.artifactory.FolderPath folderPath : folderPaths) {
                if (!folderPath.isFolder()) {
                  if (pattern.matcher(folderPath.getPath().substring(1) + folderPath.getUri()).find()) {
                    artifactPaths.add(repoKey + folderPath.getPath() + folderPath.getUri());
                  }
                }
              }
            }
          } else {
            String artifactName = pathElems[pathElems.length - 1];
            List<RepoPath> repoPaths =
                artifactory.searches().artifactsByName(artifactName).repositories(repoKey).doSearch();
            if (repoPaths != null) {
              for (RepoPath repoPath : repoPaths) {
                if (pattern.matcher(repoPath.getItemPath()).find()) {
                  artifactPaths.add(repoKey + "/" + repoPath.getItemPath());
                }
              }
            }
          }
        } else {
          folderPaths = getFolderPaths(artifactory, repoKey, "");
          if (folderPaths != null) {
            for (software.wings.helpers.ext.artifactory.FolderPath folderPath : folderPaths) {
              if (!folderPath.isFolder()) {
                if (pattern.matcher(folderPath.getUri()).find()) {
                  artifactPaths.add(repoKey + folderPath.getUri());
                }
              }
            }
          }
        }
        // Sort the alphanumeric order
        artifactPaths = artifactPaths.stream().sorted(new AlphanumComparator()).collect(toList());
        Collections.reverse(artifactPaths);
        artifactPaths = artifactPaths.stream().limit(maxVersions).collect(toList());
        Collections.reverse(artifactPaths);
      } else {
        throw new ArtifactoryServerException("Artifact path can not be empty", INVALID_ARTIFACT_SERVER);
      }
      log.info("Artifact paths order from Artifactory Server" + artifactPaths);
      return artifactPaths;
    } catch (Exception e) {
      log.error(
          format("Error occurred while retrieving File Paths from Artifactory server %s", artifactory.getUsername()),
          e);
      handleAndRethrow(e, USER);
    }
    return new ArrayList<>();
  }

  private List<software.wings.helpers.ext.artifactory.FolderPath> getFolderPaths(
      Artifactory artifactory, String repoKey, String repoPath) {
    // Add first level paths
    List<software.wings.helpers.ext.artifactory.FolderPath> folderPaths = new ArrayList<>();
    try {
      String apiStorageQuery = "api/storage/";
      apiStorageQuery = apiStorageQuery + repoKey + repoPath;
      ArtifactoryRequest repositoryRequest =
          new ArtifactoryRequestImpl().apiUrl(apiStorageQuery).method(GET).responseType(JSON);
      ArtifactoryResponse artifactoryResponse = artifactory.restCall(repositoryRequest);
      handleErrorResponse(artifactoryResponse);
      LinkedHashMap<String, Object> response = artifactoryResponse.parseBody(LinkedHashMap.class);
      if (response == null) {
        return folderPaths;
      }
      List<LinkedHashMap<String, Object>> results = (List<LinkedHashMap<String, Object>>) response.get("children");
      if (isEmpty(results)) {
        return folderPaths;
      }
      for (LinkedHashMap<String, Object> result : results) {
        folderPaths.add(FolderPath.builder()
                            .path((String) response.get("path"))
                            .uri((String) result.get("uri"))
                            .folder((boolean) result.get("folder"))
                            .build());
      }
    } catch (Exception e) {
      log.error("Exception occurred in retrieving folder paths", e);
    }
    return folderPaths;
  }

  public List<BuildDetailsInternal> getArtifactsDetails(ArtifactoryConfigRequest artifactoryConfig,
      String repositoryName, String imageName, String repositoryFormat, int maxNumberOfBuilds) {
    log.info("Retrieving artifact tags");
    List<BuildDetailsInternal> buildDetailsInternals;
    Artifactory artifactoryClient = getArtifactoryClient(artifactoryConfig);
    String artifactoryUrl = null;
    try {
      artifactoryUrl = ArtifactUtilities.getBaseUrl(artifactoryClient.getUri());
    } catch (MalformedURLException e) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Please check Artifactory connector configuration and verify that URL is valid.",
          String.format("URL [%s] is malformed.", artifactoryConfig.getArtifactoryUrl()),
          new ArtifactoryRegistryException(e.getMessage()));
    }

    ArtifactoryRequest artifactoryRequest =
        new ArtifactoryRequestImpl()
            .apiUrl("api/docker/" + repositoryName + "/v2/" + imageName + "/tags/list")
            .method(GET)
            .responseType(JSON);

    ArtifactoryResponse artifactoryResponse;

    try {
      artifactoryResponse = artifactoryClient.restCall(artifactoryRequest);
    } catch (IOException e) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Please check Artifactory connector configuration and verify that URL is valid.",
          String.format("Failed to execute API call '%s %s'", artifactoryRequest.getMethod(),
              artifactoryUrl + artifactoryRequest.getApiUrl()),
          new ArtifactoryRegistryException(e.getMessage()));
    }

    Map response;

    try {
      if (artifactoryResponse.getStatusLine().getStatusCode() == 404) {
        throw NestedExceptionUtils.hintWithExplanationException(
            "Check if the URL is correct. Check artifact configuration (repository and image field values).",
            String.format(
                "Artifactory connector URL or artifact configuration may be incorrect or the server is down or the server is not reachable from the delegate. Executed API call '%s %s' and got response code '%s'",
                artifactoryRequest.getMethod(), artifactoryUrl + artifactoryRequest.getApiUrl(),
                artifactoryResponse.getStatusLine().getStatusCode()),
            new ArtifactoryRegistryException("Artifactory Server responded with 'Not Found'.", USER));
      }
      handleErrorResponse(artifactoryResponse);
      response = artifactoryResponse.parseBody(Map.class);
    } catch (IOException e) {
      throw NestedExceptionUtils.hintWithExplanationException(
          "Please check Artifactory connector configuration and verify that URL is valid.",
          String.format("Failed to parse response for API call '%s %s' and got response code %s",
              artifactoryRequest.getMethod(), artifactoryUrl + artifactoryRequest.getApiUrl(),
              artifactoryResponse.getStatusLine().getStatusCode()),
          new ArtifactoryRegistryException(e.getMessage()));
    }

    if (!isEmpty(response)) {
      List<String> tags = (List<String>) response.get("tags");
      if (isNotEmpty(tags)) {
        String tagUrl = getBaseUrl(artifactoryConfig) + repositoryName + "/" + imageName + "/";
        String repoName = ArtifactUtilities.getArtifactoryRepositoryName(artifactoryConfig.getArtifactoryUrl(),
            artifactoryConfig.getArtifactRepositoryUrl(), repositoryName, imageName);
        buildDetailsInternals =
            tags.stream()
                .map(tag -> {
                  Map<String, String> metadata = new HashMap();
                  metadata.put(ArtifactMetadataKeys.IMAGE, repoName + ":" + tag);
                  metadata.put(ArtifactMetadataKeys.TAG, tag);
                  metadata.put(ArtifactMetadataKeys.ARTIFACT_MANIFEST_URL,
                      generateArtifactManifestUrl(artifactoryConfig, repositoryName, imageName, tag));
                  return BuildDetailsInternal.builder()
                      .number(tag)
                      .buildUrl(tagUrl + tag)
                      .metadata(metadata)
                      .uiDisplayName("Tag# " + tag)
                      .build();
                })
                .collect(toList());
        if (tags.size() < 10) {
          log.info("Retrieving image tags from artifactory url {} and repo key {} and image {} success. Tags {}",
              artifactoryUrl + artifactoryRequest.getApiUrl(), repositoryName, imageName, tags);
        } else {
          log.info("Retrieving image tags from artifactory url {} and repo key {} and image {} success. Tags {}",
              artifactoryUrl + artifactoryRequest.getApiUrl(), repositoryName, imageName, tags.size());
        }

        buildDetailsInternals.sort(new BuildDetailsInternalComparatorAscending());
        return buildDetailsInternals;
      }
    }
    throw NestedExceptionUtils.hintWithExplanationException("Please check your artifact configuration.",
        String.format("Failed to retrieve artifact tags by API call '%s %s' and got response code '%s'",
            artifactoryRequest.getMethod(), artifactoryUrl + artifactoryRequest.getApiUrl(),
            artifactoryResponse.getStatusLine().getStatusCode()),
        new ArtifactoryRegistryException(
            String.format("No tags found for artifact [repositoryFormat=%s, repository=%s, image=%s].",
                repositoryFormat, repositoryName, imageName)));
  }

  public boolean verifyArtifactManifestUrl(ArtifactoryConfigRequest artifactoryConfig, String artifactManifestUrl) {
    String authorization = null;
    if (artifactoryConfig.isHasCredentials()) {
      authorization = Credentials.basic(artifactoryConfig.getUsername(), new String(artifactoryConfig.getPassword()));
    }

    String additionalMsg = null;
    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection) new URL(artifactManifestUrl).openConnection();
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(15000);
      connection.setReadTimeout(15000);
      if (isNotEmpty(authorization)) {
        connection.setRequestProperty("Authorization", authorization);
      }
      if (connection.getResponseCode() == 200) {
        return true;
      }
    } catch (IOException e) {
      log.error("Failed to pull artifact manifest", e);
      additionalMsg = e.getMessage();
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }

    throw NestedExceptionUtils.hintWithExplanationException(
        "Please verify your Artifactory configuration artifact repository URL field.",
        String.format("Can not pull the artifact manifest. Check was performed with API call '%s %s'", "GET",
            artifactManifestUrl),
        new ArtifactoryRegistryException(
            "Could not retrieve artifact manifest." + (isNotEmpty(additionalMsg) ? additionalMsg : "")));
  }

  private String generateArtifactManifestUrl(
      ArtifactoryConfigRequest artifactoryConfig, String repositoryName, String imageName, String tag) {
    return ArtifactUtilities.getArtifactoryRegistryUrl(
               artifactoryConfig.getArtifactoryUrl(), artifactoryConfig.getArtifactRepositoryUrl(), repositoryName)
        + "/artifactory/" + ArtifactUtilities.trimSlashforwardChars(repositoryName) + "/"
        + ArtifactUtilities.trimSlashforwardChars(imageName) + "/" + ArtifactUtilities.trimSlashforwardChars(tag)
        + "/manifest.json";
  }

  @Data
  public static class ArtifactoryErrorResponse {
    List<ArtifactoryError> errors;
  }

  @Data
  public static class ArtifactoryError {
    String message;
    int status;
  }
}
