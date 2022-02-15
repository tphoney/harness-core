package software.wings.scheduler;

import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.beans.Account;
import software.wings.beans.security.UserGroup;
import software.wings.beans.sso.LdapGroupResponse;
import software.wings.beans.sso.LdapSettings;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.ldap.LdapDelegateService;

import com.google.inject.Inject;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class LdapGroupSyncJobHelperTest extends CategoryTest {
  @Inject HPersistence hPersistence;
  @InjectMocks private LdapGroupSyncJobHelper ldapGroupSyncJobHelper;
  @Mock private LdapDelegateService ldapDelegateService;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private SSOSettingService ssoSettingService;
  @Mock private UserService userService;
  @Mock private UserGroupService userGroupService;

  @Mock private FeatureFlagService featureFlagService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    ldapGroupSyncJobHelper = spy(ldapGroupSyncJobHelper);
    when(featureFlagService.isEnabled(any(), any())).thenReturn(false);
  }

  @Test
  @Owner(developers = UJJAWAL)
  @Category(UnitTests.class)
  public void shouldSyncUserGroup() {
    UserGroup userGroup = mock(UserGroup.class);
    Account account = new Account();
    String ssoId = UUIDGenerator.generateUuid();
    account.setUuid(UUIDGenerator.generateUuid());
    when(userGroup.getName()).thenReturn("userGroupName");
    when(userGroup.getAccountId()).thenReturn(UUIDGenerator.generateUuid());
    userGroup.setAccountId(UUIDGenerator.generateUuid());
    doReturn(LdapGroupResponse.builder().selectable(true).build())
        .when(ldapGroupSyncJobHelper)
        .fetchGroupDetails(any(), any(), any());
    doReturn(userGroup).when(ldapGroupSyncJobHelper).syncUserGroupMetadata(any(), any());
    doReturn(true).when(ldapGroupSyncJobHelper).validateUserGroupStates(any());
    ldapGroupSyncJobHelper.syncUserGroups(
        account.getUuid(), mock(LdapSettings.class), Collections.singletonList(userGroup), ssoId);
    verify(ssoSettingService, times(1)).closeSyncFailureAlertIfOpen(account.getUuid(), ssoId);
  }
}
