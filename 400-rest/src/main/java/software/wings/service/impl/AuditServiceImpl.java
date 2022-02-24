/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.globalcontex.AuditGlobalContextData.AUDIT_ID;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.threading.Morpheus.sleep;

import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.time.Duration.ofSeconds;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.codec.digest.DigestUtils.sha1Hex;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static org.mongodb.morphia.query.Sort.descending;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.FeatureName;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.concurrent.HTimeLimiter;
import io.harness.context.GlobalContextData;
import io.harness.delegate.beans.FileBucket;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.exception.WingsException.ExecutionContext;
import io.harness.ff.FeatureFlagService;
import io.harness.globalcontex.AuditGlobalContextData;
import io.harness.logging.ExceptionLogger;
import io.harness.manage.GlobalContextManager;
import io.harness.persistence.HIterator;
import io.harness.persistence.NameAccess;
import io.harness.persistence.UuidAccess;
import io.harness.stream.BoundedInputStream;
import io.harness.yaml.YamlUtils;

import org.omg.CORBA.INVALID_ACTIVITY;
import software.wings.app.MainConfiguration;
import software.wings.audit.AuditHeader;
import software.wings.audit.AuditHeader.AuditHeaderKeys;
import software.wings.audit.AuditHeader.RequestType;
import software.wings.audit.AuditHeaderYamlResponse;
import software.wings.audit.AuditHeaderYamlResponse.AuditHeaderYamlResponseBuilder;
import software.wings.audit.AuditRecord;
import software.wings.audit.AuditRecord.AuditRecordKeys;
import software.wings.audit.EntityAuditRecord;
import software.wings.audit.EntityAuditRecord.EntityAuditRecordBuilder;
import software.wings.audit.ResourceType;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.AuditPreference;
import software.wings.beans.EntityType;
import software.wings.beans.EntityYamlRecord;
import software.wings.beans.EntityYamlRecord.EntityYamlRecordKeys;
import software.wings.beans.Event.Type;
import software.wings.beans.HarnessTag;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.security.UserGroup;
import software.wings.common.AuditHelper;
import software.wings.dl.WingsPersistence;
import software.wings.features.AuditTrailFeature;
import software.wings.features.api.RestrictedApi;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.ApiKeyService;
import software.wings.service.intfc.AuditService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.ResourceLookupService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.settings.SettingVariableTypes;
import software.wings.yaml.YamlPayload;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.BasicDBObject;
import io.fabric8.utils.Lists;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;

/**
 * Audit Service Implementation class.
 *
 * @author Rishi
 */
@Singleton
@Slf4j
@TargetModule(HarnessModule._360_CG_MANAGER)
@OwnedBy(HarnessTeam.PL)
public class AuditServiceImpl implements AuditService {
  @Inject private FileService fileService;
  @Inject private TimeLimiter timeLimiter;
  @Inject private EntityHelper entityHelper;
  @Inject private EntityMetadataHelper entityMetadataHelper;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private EntityNameCache entityNameCache;
  @Inject private YamlResourceService yamlResourceService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private ResourceLookupService resourceLookupService;
  @Inject private AuditPreferenceHelper auditPreferenceHelper;
  @Inject private MainConfiguration configuration;
  @Inject private AccountService accountService;
  @Inject private UserService userService;
  @Inject private AuditHelper auditHelper;
  @Inject private ApiKeyService apiKeyService;

  private WingsPersistence wingsPersistence;

  private static Set<String> nonYamlEntities =
      newHashSet(EntityType.TEMPLATE_FOLDER.name(), EntityType.ENCRYPTED_RECORDS.name(),
          ResourceType.CONNECTION_ATTRIBUTES.name(), ResourceType.CUSTOM_DASHBOARD.name(),
          ResourceType.SECRET_MANAGER.name(), EntityType.PIPELINE_GOVERNANCE_STANDARD.name(),
          ResourceType.SSO_SETTINGS.name(), ResourceType.USER.name(), ResourceType.USER_INVITE.name(),
          ResourceType.DELEGATE.name(), ResourceType.DELEGATE_SCOPE.name(), ResourceType.DELEGATE_PROFILE.name());

  /**
   * check for nonYamlEntites.
   */
  boolean isNonYamlEntity(EntityAuditRecord record) {
    // If any entity is to be hidden behind feature flag it can be hidden here.
    return nonYamlEntities.contains(record.getEntityType())
        || nonYamlEntities.contains(record.getAffectedResourceType());
  }

  /**
   * Instantiates a new audit service impl.
   *
   * @param wingsPersistence the wings persistence
   */
  @Inject
  public AuditServiceImpl(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @RestrictedApi(AuditTrailFeature.class)
  public PageResponse<AuditHeader> list(PageRequest<AuditHeader> req) {
    return wingsPersistence.query(AuditHeader.class, req);
  }

  @Override
  public AuditRecord fetchMostRecentAuditRecord(String auditHeaderId) {
    return wingsPersistence.createQuery(AuditRecord.class, excludeAuthority)
        .filter(AuditRecordKeys.auditHeaderId, auditHeaderId)
        .order(Sort.descending(AuditHeaderKeys.createdAt))
        .get();
  }

  @Override
  public List<AuditRecord> fetchEntityAuditRecordsOlderThanGivenTime(String auditHeaderId, long timestamp) {
    List<AuditRecord> auditRecords = new ArrayList<>();
    try (HIterator<AuditRecord> iterator =
             new HIterator<>(wingsPersistence.createQuery(AuditRecord.class, excludeAuthority)
                                 .filter(AuditRecordKeys.auditHeaderId, auditHeaderId)
                                 .field(AuditRecordKeys.createdAt)
                                 .lessThanOrEq(timestamp)
                                 .order(Sort.ascending(AuditHeaderKeys.createdAt))
                                 .fetch())) {
      while (iterator.hasNext()) {
        auditRecords.add(iterator.next());
      }
    }
    return auditRecords;
  }

  @Override
  public void addEntityAuditRecordsToSet(
      List<EntityAuditRecord> entityAuditRecords, String accountId, String auditHeaderId) {
    UpdateOperations<AuditHeader> operations = wingsPersistence.createUpdateOperations(AuditHeader.class);
    operations.addToSet(AuditHeaderKeys.entityAuditRecords, entityAuditRecords);
    // @TODO: Following line id needed for now. Once pr for stamping accountId at auditFilter level is merged, remove
    // following line.
    operations.set(AuditHeaderKeys.accountId, accountId);
    wingsPersistence.update(wingsPersistence.createQuery(AuditHeader.class).filter(ID_KEY, auditHeaderId), operations);
  }

  @Override
  public AuditHeaderYamlResponse fetchAuditEntityYamls(String headerId, String entityId, String accountId) {
    if (isEmpty(entityId)) {
      throw new WingsException("EntityId is needed.").addParam("message", "EntityId is needed.");
    }

    AuditHeader header = wingsPersistence.createQuery(AuditHeader.class)
                             .filter(AuditHeader.ID_KEY2, headerId)
                             .project("entityAuditRecords", true)
                             .get();

    if (header == null) {
      throw new WingsException("Audit Header Id does not exists")
          .addParam("message", "Audit Header Id does not exists");
    }

    AuditHeaderYamlResponseBuilder builder =
        AuditHeaderYamlResponse.builder().auditHeaderId(headerId).entityId(entityId);

    if (isEmpty(header.getEntityAuditRecords())) {
      return builder.build();
    }

    /**
     * Under single AuditHeader record, there can be multiple entityAuditRecords with same yamlPath.
     * e.g, User updates TriggerTags, then there will be 2 records for path,
     * "Setup/Applications/Tags/Triggers/[TRIGGER].yaml"
     * 1 created for actual trigger update and 1 created for trigger-tag update, but both will use same yamlPath
     * mentioned above.
     *
     * EntityAuditRecords are stored in the list in the order of update.
     * So to show complete diff that happened to TriggerYaml in this auditOperation, we need to pick oldYamlId from 1st
     * entityAuditRecord and newYamlId from last entityAuditRecord.
     *
     * Similar also applies for serviceVariables, as there will be multiple entityAuditRecords created for yamlPath
     * Setup/Application/[APP]/Service/[SERVICE]/Index.yaml
     */
    List<EntityAuditRecord> entityAuditRecords = header.getEntityAuditRecords()
                                                     .stream()
                                                     .filter(record -> entityId.equals(record.getEntityId()))
                                                     .collect(Collectors.toList());

    if (isEmpty(entityAuditRecords)) {
      return builder.build();
    }

    String entityType = entityAuditRecords.get(0).getEntityType();
    if (ResourceType.TAG.name().equals(entityType)) {
      entityAuditRecords = header.getEntityAuditRecords()
                               .stream()
                               .filter(record -> entityType.equals(record.getEntityType()))
                               .collect(Collectors.toList());
    }

    String entityOldYamlRecordId = entityAuditRecords.get(0).getEntityOldYamlRecordId();
    String entityNewYamlRecordId = entityAuditRecords.get(entityAuditRecords.size() - 1).getEntityNewYamlRecordId();

    Set<String> yamlIds = newHashSet();
    yamlIds.add(entityOldYamlRecordId);
    yamlIds.add(entityNewYamlRecordId);

    if (isNotEmpty(yamlIds)) {
      Query<EntityYamlRecord> query = wingsPersistence.createQuery(EntityYamlRecord.class)
                                          .filter(EntityYamlRecordKeys.accountId, accountId)
                                          .field(EntityYamlRecordKeys.uuid)
                                          .in(yamlIds);
      List<EntityYamlRecord> entityAuditYamls = query.asList();
      if (isNotEmpty(entityAuditYamls)) {
        entityAuditYamls.forEach(yaml -> {
          if (yaml.getUuid().equals(entityOldYamlRecordId)) {
            builder.oldYaml(yaml.getYamlContent());
            builder.oldYamlPath(yaml.getYamlPath());
          } else if (yaml.getUuid().equals(entityNewYamlRecordId)) {
            builder.newYaml(yaml.getYamlContent());
            builder.newYamlPath(yaml.getYamlPath());
          }
        });
      }
    }

    return builder.build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AuditHeader read(String appId, String auditHeaderId) {
    return wingsPersistence.getWithAppId(AuditHeader.class, appId, auditHeaderId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AuditHeader create(AuditHeader header) {
    return wingsPersistence.saveAndGet(AuditHeader.class, header);
  }

  @Override
  public String create(AuditHeader header, RequestType requestType, InputStream inputStream) {
    String fileUuid = savePayload(header.getUuid(), requestType, inputStream);
    if (fileUuid != null) {
      UpdateOperations<AuditHeader> ops = wingsPersistence.createUpdateOperations(AuditHeader.class);
      if (requestType == RequestType.RESPONSE) {
        ops = ops.set("responsePayloadUuid", fileUuid);
      } else {
        ops = ops.set("requestPayloadUuid", fileUuid);
      }
      wingsPersistence.update(header, ops);
    }

    return fileUuid;
  }

  private String savePayload(String headerId, RequestType requestType, InputStream inputStream) {
    Map<String, Object> metaData = new HashMap<>();
    metaData.put("headerId", headerId);
    if (requestType != null) {
      metaData.put("requestType", requestType.name());
    }
    return fileService.uploadFromStream(
        requestType + "-" + headerId, new BoundedInputStream(inputStream), FileBucket.AUDITS, metaData);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void updateUser(AuditHeader header, User user) {
    if (header == null) {
      return;
    }
    Query<AuditHeader> updateQuery = wingsPersistence.createQuery(AuditHeader.class).filter(ID_KEY, header.getUuid());
    UpdateOperations<AuditHeader> updateOperations =
        wingsPersistence.createUpdateOperations(AuditHeader.class).set("remoteUser", user);
    wingsPersistence.update(updateQuery, updateOperations);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void finalize(AuditHeader header, byte[] payload) {
    AuditHeader auditHeader = wingsPersistence.get(AuditHeader.class, header.getUuid());
    UpdateOperations<AuditHeader> ops = wingsPersistence.createUpdateOperations(AuditHeader.class)
                                            .set("responseStatusCode", header.getResponseStatusCode())
                                            .set("responseTime", header.getResponseTime());
    if (configuration.getAuditConfig().isStoreResponsePayload()) {
      String fileUuid = savePayload(auditHeader.getUuid(), RequestType.RESPONSE, new ByteArrayInputStream(payload));
      if (fileUuid != null) {
        ops = ops.set("responsePayloadUuid", fileUuid);
      }
    }

    wingsPersistence.update(auditHeader, ops);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteAuditRecords(long retentionMillis) {
    final int batchSize = 1000;
    final int limit = 5000;
    final long days = TimeUnit.DAYS.convert(retentionMillis, TimeUnit.MILLISECONDS);
    log.info("Start: Deleting audit records older than {} time", currentTimeMillis() - retentionMillis);
    try {
      log.info("Start: Deleting audit records older than {} days", days);
      HTimeLimiter.callInterruptible21(timeLimiter, Duration.ofMinutes(10), () -> {
        while (true) {
          List<AuditHeader> auditHeaders = wingsPersistence.createQuery(AuditHeader.class, excludeAuthority)
                                               .field(AuditHeaderKeys.createdAt)
                                               .lessThan(currentTimeMillis() - retentionMillis)
                                               .asList(new FindOptions().limit(limit).batchSize(batchSize));
          if (isEmpty(auditHeaders)) {
            log.info("No more audit records older than {} days", days);
            return true;
          }
          try {
            log.info("Deleting {} audit records", auditHeaders.size());

            List<ObjectId> requestPayloadIds =
                auditHeaders.stream()
                    .filter(auditHeader -> auditHeader.getRequestPayloadUuid() != null)
                    .map(auditHeader -> new ObjectId(auditHeader.getRequestPayloadUuid()))
                    .collect(toList());
            List<ObjectId> responsePayloadIds =
                auditHeaders.stream()
                    .filter(auditHeader -> auditHeader.getResponsePayloadUuid() != null)
                    .map(auditHeader -> new ObjectId(auditHeader.getResponsePayloadUuid()))
                    .collect(toList());
            wingsPersistence.getCollection(DEFAULT_STORE, "audits")
                .remove(new BasicDBObject(
                    ID_KEY, new BasicDBObject("$in", auditHeaders.stream().map(AuditHeader::getUuid).toArray())));

            if (isNotEmpty(requestPayloadIds)) {
              wingsPersistence.getCollection(DEFAULT_STORE, "audits.files")
                  .remove(new BasicDBObject(ID_KEY, new BasicDBObject("$in", requestPayloadIds.toArray())));
              wingsPersistence.getCollection(DEFAULT_STORE, "audits.chunks")
                  .remove(new BasicDBObject("files_id", new BasicDBObject("$in", requestPayloadIds.toArray())));
            }

            if (isNotEmpty(requestPayloadIds)) {
              wingsPersistence.getCollection(DEFAULT_STORE, "audits.files")
                  .remove(new BasicDBObject(ID_KEY, new BasicDBObject("$in", responsePayloadIds.toArray())));
              wingsPersistence.getCollection(DEFAULT_STORE, "audits.chunks")
                  .remove(new BasicDBObject("files_id", new BasicDBObject("$in", responsePayloadIds.toArray())));
            }
          } catch (Exception ex) {
            log.warn("Failed to delete {} audit records", auditHeaders.size(), ex);
          }
          log.info("Successfully deleted {} audit records", auditHeaders.size());
          if (auditHeaders.size() < limit) {
            return true;
          }
          sleep(ofSeconds(2L));
        }
      });
    } catch (Exception ex) {
      log.warn("Failed to delete audit records older than last {} days within 10 minutes.", days, ex);
    }
    log.info("Deleted audit records older than {} days", days);
  }

  @Override
  public boolean deleteTempAuditRecords(List<String> ids) {
    return wingsPersistence.delete(
        wingsPersistence.createQuery(AuditRecord.class, excludeAuthority).field(AuditRecordKeys.uuid).in(ids));
  }

  @Override
  public <T> void handleEntityCrudOperation(String accountId, T oldEntity, T newEntity, Type type) {
    registerAuditActions(accountId, oldEntity, newEntity, type);
  }

  private Optional<String> fetchAuditHeaderIdFromGlobalContext() {
    try {
      return Optional.ofNullable(getAuditHeaderIdFromGlobalContext());
    } catch (INVALID_ACTIVITY iae) {
      return Optional.empty();
    }
  }

  private AuditHeader getAuditHeaderById(@NotNull String Id) {
    return wingsPersistence.createQuery(AuditHeader.class).filter(AuditHeader.ID_KEY2, Id).get();
  }

  private <T> void addDetails(String accountId, T entity, String auditHeaderId, Type type) {
    if (auditHeaderId == null) {
      return;
    }
    AuditHeader header = getAuditHeaderById(auditHeaderId);
    if (header == null) {
      return;
    }
    if (entity instanceof User) {
      entityMetadataHelper.addUserEntityDetails(accountId, entity, header);
    } else if (entity instanceof ApiKeyEntry && type.equals(Type.INVOKED)) {
      entityMetadataHelper.addAPIKeyDetails(accountId, entity, header);
    } else if (header.getCreatedBy() != null) {
      entityMetadataHelper.addUserDetails(accountId, entity, header);
    }
  }

  @Override
  public <T> void registerAuditActions(String accountId, T oldEntity, T newEntity, Type type) {
    try {
      Optional<String> optionalAuditHeaderId = fetchAuditHeaderIdFromGlobalContext();

      if (!optionalAuditHeaderId.isPresent()) {
        log.error(
            "AuditHeaderKey was not found from global context for accountId={} and entity={}", accountId, newEntity);
        return;
      }
      String auditHeaderId = optionalAuditHeaderId.get();
      UuidAccess entityToQuery;
      switch (type) {
        case ENABLE_2FA:
        case DISABLE_2FA:
        case LINK_SSO:
        case UNLINK_SSO:
        case MODIFY_PERMISSIONS:
        case UPDATE_NOTIFICATION_SETTING:
        case CREATE:
        case ENABLE:
        case DISABLE:
        case LOCK:
        case UNLOCK:
        case RESET_PASSWORD:
        case UPDATE_SCOPE:
        case UPDATE_TAG:
        case ACCEPTED_INVITE:
        case TEST:
        case UPDATE:
        case ADD:
        case LOGIN:
        case UNSUCCESSFUL_LOGIN:
        case LOGIN_2FA:
        case DELEGATE_APPROVAL:
        case DELEGATE_REJECTION:
        case DELEGATE_REGISTRATION:
        case NON_WHITELISTED:
        case INVOKED:
        case REMOVE:
        case APPLY:
          entityToQuery = (UuidAccess) newEntity;
          break;
        case DELETE:
          entityToQuery = (UuidAccess) oldEntity;
          break;
        default:
          log.warn(format("Unknown type class while registering audit actions: [%s]", type.getClass().getSimpleName()));
          return;
      }
      EntityAuditRecordBuilder builder = EntityAuditRecord.builder();
      entityHelper.loadMetaDataForEntity(entityToQuery, builder, type);
      EntityAuditRecord record = builder.build();
      if (featureFlagService.isEnabled(FeatureName.AUDIT_TRAIL_ENHANCEMENT, accountId)) {
        addDetails(accountId, entityToQuery, auditHeaderId, type);
      }
      updateEntityNameCacheIfRequired(oldEntity, newEntity, record);
      switch (type) {
        case LOCK:
        case UNLOCK:
        case RESET_PASSWORD:
        case ACCEPTED_INVITE:
        case ENABLE_2FA:
        case DISABLE_2FA:
        case LINK_SSO:
        case UNLINK_SSO:
        case TEST:
        case UPDATE_SCOPE:
        case ADD:
        case REMOVE:
        case DELEGATE_APPROVAL:
        case DELEGATE_REJECTION:
        case DELEGATE_REGISTRATION:
        case LOGIN:
        case UNSUCCESSFUL_LOGIN:
        case LOGIN_2FA:
        case NON_WHITELISTED:
        case INVOKED:
        case CREATE:
        case APPLY: {
          if (!(newEntity instanceof ServiceVariable) || !((ServiceVariable) newEntity).isSyncFromGit()) {
            saveEntityYamlForAudit(newEntity, record, accountId);
          }
          resourceLookupService.updateResourceLookupRecordIfNeeded(record, accountId, newEntity, oldEntity);
          break;
        }
        case ENABLE:
        case DISABLE:
        case UPDATE_TAG:
        case UPDATE_NOTIFICATION_SETTING:
        case MODIFY_PERMISSIONS:
        case UPDATE: {
          loadLatestYamlDetailsForEntity(record, accountId);
          if (!(newEntity instanceof ServiceVariable) || !((ServiceVariable) newEntity).isSyncFromGit()) {
            saveEntityYamlForAudit(newEntity, record, accountId);
          }
          resourceLookupService.updateResourceLookupRecordIfNeeded(record, accountId, newEntity, oldEntity);
          break;
        }
        case DELETE: {
          loadLatestYamlDetailsForEntity(record, accountId);
          resourceLookupService.deleteResourceLookupRecordIfNeeded(record, accountId);
          break;
        }
        default: {
          log.warn(format("Unknown type class while registering audit actions: [%s]", type.getClass().getSimpleName()));
          return;
        }
      }

      if (featureFlagService.isEnabled(FeatureName.ENTITY_AUDIT_RECORD, accountId)) {
        long now = System.currentTimeMillis();
        // Setting createdAt in EntityAuditRecord
        record.setCreatedAt(now);
        AuditRecord auditRecord = AuditRecord.builder()
                                      .auditHeaderId(auditHeaderId)
                                      .entityAuditRecord(record)
                                      .createdAt(now)
                                      .accountId(accountId)
                                      .nextIteration(now + TimeUnit.MINUTES.toMillis(3))
                                      .build();

        wingsPersistence.save(auditRecord);
      } else {
        UpdateOperations<AuditHeader> operations = wingsPersistence.createUpdateOperations(AuditHeader.class);
        operations.addToSet("entityAuditRecords", record);
        operations.set("accountId", accountId);
        wingsPersistence.update(
            wingsPersistence.createQuery(AuditHeader.class).filter(ID_KEY, auditHeaderId), operations);
      }
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, ExecutionContext.MANAGER, log);
    } catch (Exception ex) {
      log.error("Exception while auditing records for account {}", accountId, ex);
    }
  }

  @Override
  @RestrictedApi(AuditTrailFeature.class)
  public PageResponse<AuditHeader> listUsingFilter(String accountId, String filterJson, String limit, String offset) {
    AuditPreference auditPreference = (AuditPreference) auditPreferenceHelper.parseJsonIntoPreference(filterJson);
    auditPreference.setAccountId(accountId);
    changeAuditPreferenceForHomePage(auditPreference, accountId);

    if (!auditPreference.isIncludeAppLevelResources() && !auditPreference.isIncludeAccountLevelResources()) {
      return new PageResponse<>();
    }

    PageRequest<AuditHeader> pageRequest =
        auditPreferenceHelper.generatePageRequestFromAuditPreference(auditPreference, offset, limit);
    return wingsPersistence.query(AuditHeader.class, pageRequest);
  }

  @VisibleForTesting
  void changeAuditPreferenceForHomePage(AuditPreference auditPreference, String accountId) {
    if (featureFlagService.isEnabled(FeatureName.ENABLE_LOGIN_AUDITS, accountId)) {
      if (Objects.isNull(auditPreference.getApplicationAuditFilter())
          && Objects.isNull(auditPreference.getAccountAuditFilter())
          && Lists.isNullOrEmpty(auditPreference.getOperationTypes())) {
        auditPreference.setOperationTypes(Arrays.stream(Type.values()).map(Type::name).collect(Collectors.toList()));
      }

    } else {
      if (Objects.isNull(auditPreference.getApplicationAuditFilter())
          && Objects.isNull(auditPreference.getAccountAuditFilter())
          && Lists.isNullOrEmpty(auditPreference.getOperationTypes())) {
        auditPreference.setOperationTypes(Arrays.stream(Type.values())
                                              .filter(type -> type != Type.LOGIN)
                                              .filter(type -> type != Type.LOGIN_2FA)
                                              .filter(type -> type != Type.UNSUCCESSFUL_LOGIN)
                                              .map(Type::name)
                                              .collect(Collectors.toList()));
      }
    }
  }

  private <T> void updateEntityNameCacheIfRequired(T oldEntity, T newEntity, EntityAuditRecord record) {
    if (oldEntity instanceof NameAccess && newEntity instanceof NameAccess
        && !((NameAccess) oldEntity).getName().equals(((NameAccess) newEntity).getName())) {
      try {
        entityNameCache.invalidateCache(EntityType.valueOf(record.getEntityType()), record.getEntityId());
      } catch (Exception e) {
        log.warn("Failed while invalidating EntityNameCache: " + e);
      }
    }
  }

  public String getAuditHeaderIdFromGlobalContext() throws INVALID_ACTIVITY {
    GlobalContextData globalContextData;
    try {
      globalContextData = GlobalContextManager.get(AUDIT_ID);
    } catch (Exception e) {
      log.error("Exception thrown while getting audit header id ", e);
      throw new INVALID_ACTIVITY("Audit header Id not found in Global Context");
    }
    if (!(globalContextData instanceof AuditGlobalContextData)) {
      throw new INVALID_ACTIVITY("Object of unknown class returned when querying for audit header Id");
    }
    return ((AuditGlobalContextData) globalContextData).getAuditId();
  }

  @VisibleForTesting
  void loadLatestYamlDetailsForEntity(EntityAuditRecord record, String accountId) {
    if (isNonYamlEntity(record)) {
      return;
    }
    String entityId;
    String entityType;
    if (EntityType.SERVICE_VARIABLE.name().equals(record.getEntityType())) {
      if (EntityType.SERVICE.name().equals(record.getAffectedResourceType())) {
        entityType = EntityType.SERVICE.name();
      } else if (EntityType.ENVIRONMENT.name().equals(record.getAffectedResourceType())) {
        entityType = EntityType.ENVIRONMENT.name();
      } else {
        // Should ideally never happen
        return;
      }
      entityId = record.getAffectedResourceId();
    } else {
      entityType = record.getEntityType();
      entityId = record.getEntityId();
    }

    // All Tags go to same yaml file, Tags.yaml. Ignore entityId in that case and use entity type to get all tags
    // updated in this audit record.
    Query<EntityYamlRecord> query = wingsPersistence.createQuery(EntityYamlRecord.class);
    if (!ResourceType.TAG.name().equals(entityType)) {
      query.filter(EntityYamlRecordKeys.entityId, entityId);
    }

    EntityYamlRecord entityYamlRecord = query.filter(EntityYamlRecordKeys.accountId, accountId)
                                            .filter(EntityYamlRecordKeys.entityType, entityType)
                                            .project(EntityYamlRecordKeys.uuid, true)
                                            .project(EntityYamlRecordKeys.yamlPath, true)
                                            .order(descending(EntityYamlRecordKeys.createdAt))
                                            .get();

    if (entityYamlRecord != null) {
      record.setYamlPath(entityYamlRecord.getYamlPath());
      record.setEntityOldYamlRecordId(entityYamlRecord.getUuid());
    }
  }

  @VisibleForTesting
  void saveEntityYamlForAudit(Object entity, EntityAuditRecord record, String accountId) {
    if (isNonYamlEntity(record) || entity == null) {
      return;
    }
    String yamlContent;
    String yamlPath;
    try {
      if (entity instanceof ServiceVariable) {
        if (EntityType.SERVICE.name().equals(record.getAffectedResourceType())) {
          entity = serviceResourceService.getWithDetails(record.getAppId(), record.getAffectedResourceId());
        } else if (EntityType.ENVIRONMENT.name().equals(record.getAffectedResourceType())) {
          entity = environmentService.get(record.getAppId(), record.getAffectedResourceId(), false);
        } else {
          // Should ideally never happen
          return;
        }
        YamlPayload resource = yamlResourceService.obtainEntityYamlVersion(accountId, entity).getResource();
        yamlContent = resource.getYaml();
      } else if (entity instanceof ManifestFile) {
        yamlContent = ((ManifestFile) entity).getFileContent();
      } else if (entity instanceof SettingAttribute
          && SettingVariableTypes.STRING.name().equals(((SettingAttribute) entity).getValue().getType())) {
        YamlPayload resource = yamlResourceService.getDefaultVariables(accountId, record.getAppId()).getResource();
        yamlContent = resource.getYaml();
      } else if (entity instanceof HarnessTag) {
        yamlContent = yamlResourceService.getHarnessTags(accountId).getResource().getYaml();
      } else if (entity instanceof UserGroup) {
        UserGroup userGroupAudit = ((UserGroup) entity).buildUserGroupAudit();
        yamlContent = toYamlString(userGroupAudit);
      } else {
        YamlPayload resource = yamlResourceService.obtainEntityYamlVersion(accountId, entity).getResource();
        yamlContent = resource.getYaml();
      }
      yamlPath = entityHelper.getFullYamlPathForEntity(entity, record);
      record.setYamlPath(yamlPath);
    } catch (Exception ex) {
      yamlContent =
          format("Exception: [%s] while generating Yamls for entityId: [%s], entityType: [%s], accountId: [%s]",
              ex.getMessage(), record.getEntityId(), record.getEntityType(), accountId);
      yamlPath = EMPTY;
      log.error(yamlContent, ex);
    }
    String entityId;
    String entityType;
    if (EntityType.SERVICE_VARIABLE.name().equals(record.getEntityType())) {
      if (EntityType.SERVICE.name().equals(record.getAffectedResourceType())) {
        entityType = EntityType.SERVICE.name();
      } else if (EntityType.ENVIRONMENT.name().equals(record.getAffectedResourceType())) {
        entityType = EntityType.ENVIRONMENT.name();
      } else {
        // Should ideally never happen
        return;
      }
      entityId = record.getAffectedResourceId();
    } else {
      entityType = record.getEntityType();
      entityId = record.getEntityId();
    }
    EntityYamlRecord yamlRecord = EntityYamlRecord.builder()
                                      .uuid(generateUuid())
                                      .accountId(accountId)
                                      .createdAt(currentTimeMillis())
                                      .entityId(entityId)
                                      .entityType(entityType)
                                      .yamlPath(yamlPath)
                                      .yamlSha(sha1Hex(yamlContent))
                                      .yamlContent(yamlContent)
                                      .build();
    String newYamlId = wingsPersistence.save(yamlRecord);
    record.setEntityNewYamlRecordId(newYamlId);
  }

  public static String toYamlString(Object theYaml) {
    String connectorString;
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    try {
      connectorString = YamlUtils.cleanupYaml(mapper.writeValueAsString(theYaml));
    } catch (Exception ex) {
      throw new InvalidRequestException("Encountered exception while serializing user group " + ex.getMessage());
    }
    return connectorString;
  }
}
