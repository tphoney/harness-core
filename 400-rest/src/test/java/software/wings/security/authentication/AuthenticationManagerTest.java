/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security.authentication;

import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.eraro.ErrorCode.USER_DOES_NOT_EXIST;
import static io.harness.exception.WingsException.USER;
import static io.harness.ng.core.account.AuthenticationMechanism.USER_PASSWORD;
import static io.harness.ng.core.account.DefaultExperience.CG;
import static io.harness.ng.core.account.DefaultExperience.NG;
import static io.harness.rule.OwnerRule.AMAN;
import static io.harness.rule.OwnerRule.LAZAR;
import static io.harness.rule.OwnerRule.PHOENIKX;
import static io.harness.rule.OwnerRule.RUSHABH;
import static io.harness.rule.OwnerRule.UTKARSH;
import static io.harness.rule.OwnerRule.VIKAS;
import static io.harness.rule.OwnerRule.VIKAS_M;

import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.beans.User.Builder.anUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.configuration.DeployMode;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidCredentialsException;
import io.harness.exception.WingsException;
import io.harness.ng.core.account.AuthenticationMechanism;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rule.Owner;
import io.harness.usermembership.remote.UserMembershipClient;

import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.app.PortalConfig;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.security.UserPermissionInfo;
import software.wings.security.authentication.recaptcha.FailedLoginAttemptCountChecker;
import software.wings.security.authentication.recaptcha.MaxLoginAttemptExceededException;
import software.wings.security.saml.SSORequest;
import software.wings.security.saml.SamlClientService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.UserService;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import retrofit2.Call;
import retrofit2.Response;

@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public class AuthenticationManagerTest extends WingsBaseTest {
  private static final String NON_EXISTING_USER = "nonExistingUser";
  private static final String PASSWORD_WITH_SPECIAL_CHARECTERS = "prefix:suffix:abc_+=./,!@#$%^&&*(Z)";
  private static final String TEST_TOKEN = "TestToken";
  private static final String USER_NAME = "testUser@test.com";
  private static final String UUID = "TestUUID";
  @Mock private PasswordBasedAuthHandler PASSWORD_BASED_AUTH_HANDLER;
  @Mock private SamlClientService SAML_CLIENT_SERVICE;
  @Mock private MainConfiguration MAIN_CONFIGURATION;
  @Mock private UserService USER_SERVICE;
  @Mock private AuthenticationUtils AUTHENTICATION_UTL;
  @Mock private AuthService AUTHSERVICE;
  @Mock private AccountService accountService;
  @Mock private UserMembershipClient userMembershipClient;
  @Mock private FailedLoginAttemptCountChecker failedLoginAttemptCountChecker;

  @Captor ArgumentCaptor<String> argCaptor;

  @Inject @InjectMocks private AuthenticationManager authenticationManager;

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void getAuthenticationMechanism() {
    User mockUser = mock(User.class);
    Account account1 = mock(Account.class);
    Account account2 = mock(Account.class);

    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1, account2));
    when(AUTHENTICATION_UTL.getUser("testUser", WingsException.USER)).thenReturn(mockUser);
    assertThat(authenticationManager.getAuthenticationMechanism("testUser")).isEqualTo(USER_PASSWORD);

    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1));
    when(account1.getAuthenticationMechanism()).thenReturn(USER_PASSWORD);
    assertThat(authenticationManager.getAuthenticationMechanism("testUser")).isEqualTo(USER_PASSWORD);

    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1));
    when(account1.getAuthenticationMechanism()).thenReturn(io.harness.ng.core.account.AuthenticationMechanism.SAML);
    assertThat(authenticationManager.getAuthenticationMechanism("testUser"))
        .isEqualTo(io.harness.ng.core.account.AuthenticationMechanism.SAML);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testDefaultLoginAccountForInvalidUserinSaaS() {
    when(MAIN_CONFIGURATION.getDeployMode()).thenReturn(DeployMode.KUBERNETES);
    testLoginAttemptForInvalidUser();
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testDefaultLoginAccountForInvalidUserinOnPrem() {
    when(MAIN_CONFIGURATION.getDeployMode()).thenReturn(DeployMode.ONPREM);
    testLoginAttemptForInvalidUser();
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testDefaultLoginAccountForInvalidUserinKubernetesOnPrem() {
    when(MAIN_CONFIGURATION.getDeployMode()).thenReturn(DeployMode.KUBERNETES_ONPREM);
    testLoginAttemptForInvalidUser();
  }

  private void testLoginAttemptForInvalidUser() {
    PortalConfig portalConfig = mock(PortalConfig.class);
    when(portalConfig.getAuthTokenExpiryInMillis()).thenReturn(System.currentTimeMillis());
    when(MAIN_CONFIGURATION.getPortal()).thenReturn(portalConfig);

    String userName = "testUser@test.com";

    String basicToken = Base64.encodeBase64String((userName + ":password").getBytes());

    when(AUTHENTICATION_UTL.getUser(Matchers.eq(userName), Matchers.eq(USER)))
        .thenThrow(new WingsException(ErrorCode.USER_DOES_NOT_EXIST));

    assertThatThrownBy(() -> authenticationManager.defaultLoginAccount(basicToken, "testAccontId"))
        .isInstanceOf(InvalidCredentialsException.class)
        .matches(ex -> ((WingsException) ex).getCode() == INVALID_CREDENTIAL);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetLoginTypeResponseForInvalidUserInSaaS() {
    when(MAIN_CONFIGURATION.getDeployMode()).thenReturn(DeployMode.KUBERNETES);
    when(AUTHENTICATION_UTL.getUser(Matchers.same(NON_EXISTING_USER), any(EnumSet.class)))
        .thenThrow(new WingsException(ErrorCode.USER_DOES_NOT_EXIST));
    assertThatThrownBy(() -> authenticationManager.getLoginTypeResponse(NON_EXISTING_USER))
        .isInstanceOf(WingsException.class)
        .matches(ex -> ((WingsException) ex).getCode() == USER_DOES_NOT_EXIST);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void getLoginTypeResponse() {
    User mockUser = mock(User.class);
    Account account1 = mock(Account.class);
    Account account2 = mock(Account.class);

    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1, account2));
    when(mockUser.isEmailVerified()).thenReturn(true);

    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1, account2));
    when(AUTHENTICATION_UTL.getUser(Matchers.anyString(), any(EnumSet.class))).thenReturn(mockUser);
    when(USER_SERVICE.getAccountByIdIfExistsElseGetDefaultAccount(any(User.class), Optional.of(anyString())))
        .thenReturn(account1);
    LoginTypeResponse loginTypeResponse = authenticationManager.getLoginTypeResponse("testUser");
    assertThat(loginTypeResponse.getAuthenticationMechanism()).isEqualTo(USER_PASSWORD);
    assertThat(loginTypeResponse.getSSORequest()).isNull();

    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1, account2));
    when(AUTHENTICATION_UTL.getUser("testUser", WingsException.USER)).thenReturn(mockUser);
    assertThat(authenticationManager.getAuthenticationMechanism("testUser")).isEqualTo(USER_PASSWORD);

    loginTypeResponse = authenticationManager.getLoginTypeResponse("testUser");
    assertThat(loginTypeResponse.getAuthenticationMechanism()).isEqualTo(USER_PASSWORD);
    assertThat(loginTypeResponse.getSSORequest()).isNull();

    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1));
    when(account1.getAuthenticationMechanism()).thenReturn(io.harness.ng.core.account.AuthenticationMechanism.SAML);
    SSORequest SSORequest = new SSORequest();
    SSORequest.setIdpRedirectUrl("TestURL");
    when(SAML_CLIENT_SERVICE.generateSamlRequestFromAccount(account1, false)).thenReturn(SSORequest);
    loginTypeResponse = authenticationManager.getLoginTypeResponse("testUser");
    assertThat(loginTypeResponse.getAuthenticationMechanism()).isEqualTo(AuthenticationMechanism.SAML);
    assertThat(loginTypeResponse.getSSORequest()).isNotNull();
    SSORequest receivedRequest = loginTypeResponse.getSSORequest();
    assertThat(receivedRequest.getIdpRedirectUrl()).isEqualTo("TestURL");
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testGetLoginType_emailUnverified_shouldFail() throws MaxLoginAttemptExceededException {
    User mockUser = mock(User.class);
    Account account1 = mock(Account.class);

    doNothing().when(failedLoginAttemptCountChecker).check(Mockito.any(User.class));

    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1));
    when(USER_SERVICE.getAccountByIdIfExistsElseGetDefaultAccount(any(User.class), Optional.of(anyString())))
        .thenReturn(account1);
    when(AUTHENTICATION_UTL.getUser(Matchers.anyString(), any(EnumSet.class))).thenReturn(mockUser);
    try {
      authenticationManager.getLoginTypeResponse("testUser");
      fail("Exception is expected if the user email is not verified.");
    } catch (WingsException e) {
      // Exception expected.
    }
  }

  @Test
  @Owner(developers = LAZAR)
  @Category(UnitTests.class)
  public void testGetLoginType_emailUnverified() throws MaxLoginAttemptExceededException {
    User mockUser = mock(User.class);

    doNothing().when(failedLoginAttemptCountChecker).check(Mockito.any(User.class));
    when(mockUser.getAccounts()).thenReturn(Collections.emptyList());
    when(AUTHENTICATION_UTL.getUser(Matchers.anyString(), any(EnumSet.class))).thenReturn(mockUser);

    LoginTypeResponse loginTypeResponse = authenticationManager.getLoginTypeResponse("testUser");

    assertThat(loginTypeResponse.getAuthenticationMechanism()).isEqualTo(USER_PASSWORD);
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void authenticate() {
    User mockUser = spy(new User());
    AuthenticationResponse authenticationResponse = spy(new AuthenticationResponse(mockUser));
    mockUser.setUuid("TestUUID");
    PortalConfig portalConfig = mock(PortalConfig.class);
    when(portalConfig.getAuthTokenExpiryInMillis()).thenReturn(System.currentTimeMillis());
    when(MAIN_CONFIGURATION.getPortal()).thenReturn(portalConfig);
    Account account1 = mock(Account.class);
    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1));
    when(AUTHENTICATION_UTL.getUser("testUser@test.com", WingsException.USER)).thenReturn(mockUser);

    when(PASSWORD_BASED_AUTH_HANDLER.authenticate(Matchers.anyString(), Matchers.anyString()))
        .thenReturn(authenticationResponse);
    User authenticatedUser = mock(User.class);
    when(authenticatedUser.getToken()).thenReturn("TestToken");
    when(AUTHSERVICE.generateBearerTokenForUser(mockUser)).thenReturn(authenticatedUser);
    User user = authenticationManager.defaultLogin(Base64.encodeBase64String("testUser@test.com:password".getBytes()));
    assertThat(user.getToken()).isEqualTo("TestToken");
    assertThat(authenticatedUser.getLastLogin() != 0L);
    assertThat(authenticatedUser.getLastLogin() <= System.currentTimeMillis());
  }

  @Test
  @Owner(developers = AMAN)
  @Category(UnitTests.class)
  public void testCredentialDecoding() {
    User mockUser = spy(new User());
    AuthenticationResponse authenticationResponse = spy(new AuthenticationResponse(mockUser));
    mockUser.setUuid(UUID);
    PortalConfig portalConfig = mock(PortalConfig.class);
    when(portalConfig.getAuthTokenExpiryInMillis()).thenReturn(System.currentTimeMillis());
    when(MAIN_CONFIGURATION.getPortal()).thenReturn(portalConfig);
    Account account1 = mock(Account.class);
    when(mockUser.getAccounts()).thenReturn(Arrays.asList(account1));
    when(AUTHENTICATION_UTL.getUser(USER_NAME, WingsException.USER)).thenReturn(mockUser);

    when(PASSWORD_BASED_AUTH_HANDLER.authenticate(Matchers.anyString(), Matchers.anyString()))
        .thenReturn(authenticationResponse);
    User authenticatedUser = mock(User.class);
    when(authenticatedUser.getToken()).thenReturn(TEST_TOKEN);
    when(AUTHSERVICE.generateBearerTokenForUser(mockUser)).thenReturn(authenticatedUser);

    // trying with testUser@test.com:prefix:suffix:abc
    String password = PASSWORD_WITH_SPECIAL_CHARECTERS;
    User user = authenticationManager.defaultLogin(Base64.encodeBase64String((USER_NAME + ":" + password).getBytes()));

    verify(PASSWORD_BASED_AUTH_HANDLER, times(1)).authenticate(argCaptor.capture());

    assertThat(USER_NAME).isEqualTo(argCaptor.getAllValues().get(0));
    assertThat(password).isEqualTo(argCaptor.getAllValues().get(1));
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void testFakeTokens() {
    try {
      authenticationManager.defaultLogin("FakeToken");
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.INVALID_CREDENTIAL.name());
    }

    try {
      authenticationManager.defaultLogin(Base64.encodeBase64String("testUser@test.com".getBytes()));
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.INVALID_CREDENTIAL.name());
    }
  }

  @Test
  @Owner(developers = RUSHABH)
  @Category(UnitTests.class)
  public void extractToken() {
    try {
      authenticationManager.extractToken("fakeData", "Basic");
      failBecauseExceptionWasNotThrown(WingsException.class);
    } catch (WingsException e) {
      assertThat(e.getMessage()).isEqualTo(ErrorCode.INVALID_TOKEN.name());
    }

    String token = authenticationManager.extractToken("Basic testData", "Basic");
    assertThat(token).isEqualTo("testData");
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void testGetLoginTypeResponseForInvalidUserForOnPrem() throws IllegalAccessException {
    when(MAIN_CONFIGURATION.getDeployMode()).thenReturn(DeployMode.ONPREM);
    FieldUtils.writeDeclaredField(authenticationManager, "mainConfiguration", MAIN_CONFIGURATION, true);
    testForInvalidUserInOnPrem();
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  public void getLoginTypeResponseForInvalidUserForKubernetesOnPrem() throws IllegalAccessException {
    when(MAIN_CONFIGURATION.getDeployMode()).thenReturn(DeployMode.KUBERNETES_ONPREM);
    FieldUtils.writeDeclaredField(authenticationManager, "mainConfiguration", MAIN_CONFIGURATION, true);
    testForInvalidUserInOnPrem();
  }

  private void testForInvalidUserInOnPrem() {
    when(MAIN_CONFIGURATION.getDeployMode()).thenReturn(DeployMode.KUBERNETES_ONPREM);
    when(AUTHENTICATION_UTL.getUser(Matchers.same(NON_EXISTING_USER), any(EnumSet.class)))
        .thenThrow(new WingsException(ErrorCode.USER_DOES_NOT_EXIST));

    LoginTypeResponse loginTypeResponse = authenticationManager.getLoginTypeResponse(NON_EXISTING_USER);
    assertThat(loginTypeResponse).isNotNull();
    assertThat(loginTypeResponse.getAuthenticationMechanism()).isEqualTo(USER_PASSWORD);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testAuditForDefaultLogin() {
    Account account = anAccount().withUuid("AccountId1").build();
    AuthenticationManager spyAuthenticationManager = spy(authenticationManager);
    User userToBeReturned = anUser()
                                .twoFactorAuthenticationEnabled(false)
                                .defaultAccountId("AccountId1")
                                .uuid("User")
                                .accounts(Lists.newArrayList(account))
                                .build();

    when(PASSWORD_BASED_AUTH_HANDLER.authenticate(any(), any()))
        .thenReturn(new AuthenticationResponse(userToBeReturned));
    doReturn(USER_PASSWORD).when(spyAuthenticationManager).getAuthenticationMechanism(any());
    when(AUTHSERVICE.generateBearerTokenForUser(any())).thenReturn(userToBeReturned);
    doNothing().when(AUTHSERVICE).auditLogin(any(), any());

    spyAuthenticationManager.defaultLogin("abcd", "abcd");
    verify(AUTHSERVICE).auditLogin(any(), any());
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testAuditForLoginWithSso() {
    Account account = anAccount().withUuid("AccountId1").build();
    AuthenticationManager spyAuthenticationManager = spy(authenticationManager);
    User userToBeReturned = anUser()
                                .twoFactorAuthenticationEnabled(false)
                                .defaultAccountId("AccountId1")
                                .uuid("User")
                                .accounts(Lists.newArrayList(account))
                                .build();

    when(USER_SERVICE.verifyJWTToken(anyString(), any())).thenReturn(userToBeReturned);
    when(AUTHSERVICE.generateBearerTokenForUser(any())).thenReturn(userToBeReturned);
    doNothing().when(AUTHSERVICE).auditLogin(any(), any());

    spyAuthenticationManager.ssoRedirectLogin("abcd");

    verify(AUTHSERVICE).auditLogin(any(), any());
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testHarnessLocalLoginNgAdmin() throws IOException {
    String accountId = "AccountId1";
    Account account = anAccount().withUuid(accountId).withDefaultExperience(NG).build();
    User mockUser = mock(User.class);
    User authenticatedUser = mock(User.class);
    String basicToken = Base64.encodeBase64String(("UserName"
        + ":password")
                                                      .getBytes());
    AuthenticationResponse authenticationResponse = spy(new AuthenticationResponse(mockUser));
    when(PASSWORD_BASED_AUTH_HANDLER.authenticate(Matchers.anyString(), Matchers.anyString()))
        .thenReturn(authenticationResponse);
    when(AUTHSERVICE.generateBearerTokenForUser(mockUser)).thenReturn(authenticatedUser);
    doNothing().when(AUTHSERVICE).auditLogin(any(), any());
    when(accountService.get(accountId)).thenReturn(account);
    Call<ResponseDTO<Boolean>> request = mock(Call.class);
    doReturn(request).when(userMembershipClient).isUserAdmin(anyString(), anyString());
    ResponseDTO<Boolean> mockResponse = ResponseDTO.newResponse(true);
    doReturn(Response.success(mockResponse)).when(request).execute();
    assertThat(authenticationManager.loginUsingHarnessPassword(basicToken, accountId)).isEqualTo(authenticatedUser);
  }

  @Test(expected = WingsException.class)
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testHarnessLocalLoginNgNotAdmin() throws IOException {
    String accountId = "AccountId1";
    Account account = anAccount().withUuid(accountId).withDefaultExperience(NG).build();
    User mockUser = mock(User.class);
    User authenticatedUser = mock(User.class);
    String basicToken = Base64.encodeBase64String(("UserName"
        + ":password")
                                                      .getBytes());
    AuthenticationResponse authenticationResponse = spy(new AuthenticationResponse(mockUser));
    when(PASSWORD_BASED_AUTH_HANDLER.authenticate(Matchers.anyString(), Matchers.anyString()))
        .thenReturn(authenticationResponse);
    when(AUTHSERVICE.generateBearerTokenForUser(mockUser)).thenReturn(authenticatedUser);
    doNothing().when(AUTHSERVICE).auditLogin(any(), any());
    when(accountService.get(accountId)).thenReturn(account);
    Call<ResponseDTO<Boolean>> request = mock(Call.class);
    doReturn(request).when(userMembershipClient).isUserAdmin(anyString(), anyString());
    ResponseDTO<Boolean> mockResponse = ResponseDTO.newResponse(false);
    doReturn(Response.success(mockResponse)).when(request).execute();
    authenticationManager.loginUsingHarnessPassword(basicToken, accountId);
  }

  @Test
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testHarnessLocalLoginCgAdmin() {
    String accountId = "AccountId1";
    Account account = anAccount().withUuid(accountId).withDefaultExperience(CG).build();
    User mockUser = mock(User.class);
    User authenticatedUser = mock(User.class);
    String basicToken = Base64.encodeBase64String(("UserName"
        + ":password")
                                                      .getBytes());
    AuthenticationResponse authenticationResponse = spy(new AuthenticationResponse(mockUser));
    when(PASSWORD_BASED_AUTH_HANDLER.authenticate(Matchers.anyString(), Matchers.anyString()))
        .thenReturn(authenticationResponse);
    when(AUTHSERVICE.generateBearerTokenForUser(mockUser)).thenReturn(authenticatedUser);
    doNothing().when(AUTHSERVICE).auditLogin(any(), any());
    when(accountService.get(accountId)).thenReturn(account);
    UserPermissionInfo userPermissionInfo = UserPermissionInfo.builder().build();
    when(AUTHSERVICE.getUserPermissionInfo(accountId, authenticatedUser, false)).thenReturn(userPermissionInfo);
    when(USER_SERVICE.isUserAccountAdmin(any(), any())).thenReturn(true);
    assertThat(authenticationManager.loginUsingHarnessPassword(basicToken, accountId)).isEqualTo(authenticatedUser);
  }

  @Test(expected = WingsException.class)
  @Owner(developers = VIKAS_M)
  @Category(UnitTests.class)
  public void testHarnessLocalLoginCgNonAdmin() {
    String accountId = "AccountId1";
    Account account = anAccount().withUuid(accountId).withDefaultExperience(CG).build();
    User mockUser = mock(User.class);
    User authenticatedUser = mock(User.class);
    String basicToken = Base64.encodeBase64String(("UserName"
        + ":password")
                                                      .getBytes());
    AuthenticationResponse authenticationResponse = spy(new AuthenticationResponse(mockUser));
    when(PASSWORD_BASED_AUTH_HANDLER.authenticate(Matchers.anyString(), Matchers.anyString()))
        .thenReturn(authenticationResponse);
    when(AUTHSERVICE.generateBearerTokenForUser(mockUser)).thenReturn(authenticatedUser);
    doNothing().when(AUTHSERVICE).auditLogin(any(), any());
    when(accountService.get(accountId)).thenReturn(account);
    UserPermissionInfo userPermissionInfo = UserPermissionInfo.builder().build();
    when(AUTHSERVICE.getUserPermissionInfo(accountId, authenticatedUser, false)).thenReturn(userPermissionInfo);
    when(USER_SERVICE.isUserAccountAdmin(any(), any())).thenReturn(false);
    assertThat(authenticationManager.loginUsingHarnessPassword(basicToken, accountId)).isEqualTo(WingsException.class);
  }
}
