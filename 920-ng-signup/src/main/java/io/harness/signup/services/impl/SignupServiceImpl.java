/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.signup.services.impl;

import static io.harness.annotations.dev.HarnessTeam.GTM;
import static io.harness.configuration.DeployMode.DEPLOY_MODE;
import static io.harness.configuration.DeployVariant.DEPLOY_VERSION;
import static io.harness.exception.WingsException.USER;
import static io.harness.remote.client.RestClientUtils.getResponse;
import static io.harness.signup.services.SignupType.COMMUNITY_PROVISION;
import static io.harness.utils.CryptoUtils.secureRandAlphaNumString;

import static java.lang.Boolean.FALSE;
import static org.mindrot.jbcrypt.BCrypt.hashpw;

import io.harness.ModuleType;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.accesscontrol.acl.api.Resource;
import io.harness.accesscontrol.acl.api.ResourceScope;
import io.harness.account.services.AccountService;
import io.harness.annotations.dev.OwnedBy;
import io.harness.authenticationservice.recaptcha.ReCaptchaVerifier;
import io.harness.configuration.DeployMode;
import io.harness.configuration.DeployVariant;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.SignupException;
import io.harness.exception.UserAlreadyPresentException;
import io.harness.exception.WeakPasswordException;
import io.harness.exception.WingsException;
import io.harness.licensing.services.LicenseService;
import io.harness.ng.core.dto.AccountDTO;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.UserRequestDTO;
import io.harness.ng.core.user.UtmInfo;
import io.harness.notification.templates.PredefinedTemplate;
import io.harness.repositories.SignupVerificationTokenRepository;
import io.harness.signup.dto.OAuthSignupDTO;
import io.harness.signup.dto.SignupDTO;
import io.harness.signup.dto.SignupInviteDTO;
import io.harness.signup.dto.VerifyTokenResponseDTO;
import io.harness.signup.entities.SignupVerificationToken;
import io.harness.signup.notification.EmailType;
import io.harness.signup.notification.SignupNotificationHelper;
import io.harness.signup.services.SignupService;
import io.harness.signup.services.SignupType;
import io.harness.signup.validator.SignupValidator;
import io.harness.telemetry.Category;
import io.harness.telemetry.Destination;
import io.harness.telemetry.TelemetryOption;
import io.harness.telemetry.TelemetryReporter;
import io.harness.user.remote.UserClient;
import io.harness.version.VersionInfoManager;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URIBuilder;
import org.mindrot.jbcrypt.BCrypt;

@Slf4j
@Singleton
@OwnedBy(GTM)
public class SignupServiceImpl implements SignupService {
  private AccountService accountService;
  private UserClient userClient;
  private SignupValidator signupValidator;
  private ReCaptchaVerifier reCaptchaVerifier;
  private final TelemetryReporter telemetryReporter;
  private final SignupNotificationHelper signupNotificationHelper;
  private final SignupVerificationTokenRepository verificationTokenRepository;
  private final ExecutorService executorService;
  private final AccessControlClient accessControlClient;
  private final LicenseService licenseService;
  private final VersionInfoManager versionInfoManager;

  public static final String FAILED_EVENT_NAME = "SIGNUP_ATTEMPT_FAILED";
  public static final String SUCCEED_EVENT_NAME = "NEW_SIGNUP";
  public static final String SUCCEED_SIGNUP_INVITE_NAME = "SIGNUP_VERIFY";
  private static final String VERIFY_URL = "/register/verify/%s?email=%s";
  private static final String LOGIN_URL = "/signin";
  private static final int SIGNUP_TOKEN_VALIDITY_IN_DAYS = 30;
  private static final String UNDEFINED_ACCOUNT_ID = "undefined";
  private static final String NG_AUTH_UI_PATH_PREFIX = "auth/";

  private static String deployVersion = System.getenv().get(DEPLOY_VERSION);

  @Inject
  public SignupServiceImpl(AccountService accountService, UserClient userClient, SignupValidator signupValidator,
      ReCaptchaVerifier reCaptchaVerifier, TelemetryReporter telemetryReporter,
      SignupNotificationHelper signupNotificationHelper, SignupVerificationTokenRepository verificationTokenRepository,
      @Named("NGSignupNotification") ExecutorService executorService,
      @Named("PRIVILEGED") AccessControlClient accessControlClient, LicenseService licenseService,
      VersionInfoManager versionInfoManager) {
    this.accountService = accountService;
    this.userClient = userClient;
    this.signupValidator = signupValidator;
    this.reCaptchaVerifier = reCaptchaVerifier;
    this.telemetryReporter = telemetryReporter;
    this.signupNotificationHelper = signupNotificationHelper;
    this.verificationTokenRepository = verificationTokenRepository;
    this.executorService = executorService;
    this.accessControlClient = accessControlClient;
    this.licenseService = licenseService;
    this.versionInfoManager = versionInfoManager;
  }

  /**
   * Signup in non email verification blocking flow
   */
  @Override
  public UserInfo signup(SignupDTO dto, String captchaToken) throws WingsException {
    verifyReCaptcha(dto, captchaToken);
    verifySignupDTO(dto);

    dto.setEmail(dto.getEmail().toLowerCase());

    AccountDTO account = createAccount(dto);
    UserInfo user = createUser(dto, account);
    sendSucceedTelemetryEvent(
        dto.getEmail(), dto.getUtmInfo(), account.getIdentifier(), user, SignupType.SIGNUP_FORM_FLOW);
    executorService.submit(() -> {
      SignupVerificationToken verificationToken = generateNewToken(user.getEmail());
      try {
        String url = generateVerifyUrl(user.getDefaultAccountId(), verificationToken.getToken(), dto.getEmail());
        signupNotificationHelper.sendSignupNotification(
            user, EmailType.VERIFY, PredefinedTemplate.EMAIL_VERIFY.getIdentifier(), url);
      } catch (URISyntaxException e) {
        log.error("Failed to generate verify url", e);
      }
    });
    return user;
  }

  /**
   * Signup in non email verification blocking flow
   */
  @Override
  public UserInfo communitySignup(SignupDTO dto) throws WingsException {
    String deployMode = System.getenv().get(DEPLOY_MODE);
    if (!DeployMode.isOnPrem(deployMode)) {
      throw new InvalidRequestException("Deploy mode is not on prem", ErrorCode.DEPLOY_MODE_IS_NOT_ON_PREM, USER);
    }

    if (!DeployVariant.isCommunity(deployVersion)) {
      throw new InvalidRequestException("Community edition not found", ErrorCode.COMMNITY_EDITION_NOT_FOUND, USER);
    }

    verifySignupDTO(dto);

    dto.setEmail(dto.getEmail().toLowerCase());

    String passwordHash = hashpw(dto.getPassword(), BCrypt.gensalt());
    SignupInviteDTO signupRequest = SignupInviteDTO.builder()
                                        .email(dto.getEmail())
                                        .passwordHash(passwordHash)
                                        .intent(dto.getIntent())
                                        .signupAction(dto.getSignupAction())
                                        .edition(dto.getEdition())
                                        .billingFrequency(dto.getBillingFrequency())
                                        .build();

    UserInfo userInfo = null;
    try {
      userInfo = getResponse(userClient.createCommunityUserAndCompleteSignup(signupRequest));
    } catch (InvalidRequestException e) {
      if (e.getMessage().contains("User with this email is already registered")) {
        throw new InvalidRequestException("Email is already signed up", ErrorCode.USER_ALREADY_REGISTERED, USER);
      }
      throw e;
    } catch (Exception e) {
      log.error("Unable to finish community provision flow", e);
      throw e;
    }

    licenseService.startCommunityLicense(userInfo.getDefaultAccountId(), ModuleType.CD);
    sendCommunitySucceedTelemetry(userInfo.getEmail(), userInfo.getDefaultAccountId(), userInfo, COMMUNITY_PROVISION);

    waitForRbacSetup(userInfo.getDefaultAccountId(), userInfo.getUuid(), userInfo.getEmail());
    return userInfo;
  }

  /**
   * Signup Invite in email verification blocking flow
   */
  @Override
  public boolean createSignupInvite(SignupDTO dto, String captchaToken) {
    if (DeployVariant.isCommunity(deployVersion)) {
      throw new InvalidRequestException("You are not allowed to create a signup invite with community edition");
    }

    verifyReCaptcha(dto, captchaToken);
    verifySignupDTO(dto);

    dto.setEmail(dto.getEmail().toLowerCase());

    String passwordHash = hashpw(dto.getPassword(), BCrypt.gensalt());
    SignupInviteDTO signupRequest = SignupInviteDTO.builder()
                                        .email(dto.getEmail())
                                        .passwordHash(passwordHash)
                                        .intent(dto.getIntent())
                                        .signupAction(dto.getSignupAction())
                                        .edition(dto.getEdition())
                                        .billingFrequency(dto.getBillingFrequency())
                                        .utmInfo(dto.getUtmInfo())
                                        .build();
    try {
      getResponse(userClient.createNewSignupInvite(signupRequest));
    } catch (InvalidRequestException e) {
      sendFailedTelemetryEvent(dto.getEmail(), dto.getUtmInfo(), e, null, "Create Signup Invite");
      if (e.getMessage().contains("User with this email is already registered")) {
        throw new InvalidRequestException("Email is already signed up", ErrorCode.USER_ALREADY_REGISTERED, USER);
      }
      throw e;
    }

    sendSucceedInvite(dto.getEmail(), dto.getUtmInfo());
    executorService.submit(() -> {
      SignupVerificationToken verificationToken = generateNewToken(dto.getEmail());
      try {
        String url = generateVerifyUrl(null, verificationToken.getToken(), dto.getEmail());
        signupNotificationHelper.sendSignupNotification(
            UserInfo.builder().email(dto.getEmail()).defaultAccountId(UNDEFINED_ACCOUNT_ID).build(), EmailType.VERIFY,
            PredefinedTemplate.EMAIL_VERIFY.getIdentifier(), url);
      } catch (URISyntaxException e) {
        log.error("Failed to generate verify url", e);
      }
    });
    log.info("Created NG signup invite for {}", dto.getEmail());
    return true;
  }

  /**
   * Complete Signup in email verification blocking flow
   */
  @Override
  public UserInfo completeSignupInvite(String token) {
    if (DeployVariant.isCommunity(deployVersion)) {
      throw new InvalidRequestException("You are not allowed to complete a signup invite with community edition");
    }

    Optional<SignupVerificationToken> verificationTokenOptional = verificationTokenRepository.findByToken(token);

    if (!verificationTokenOptional.isPresent()) {
      throw new InvalidRequestException("Email token doesn't exist");
    }

    SignupVerificationToken verificationToken = verificationTokenOptional.get();

    if (verificationToken.getValidUntil() == null || verificationToken.getUserId() != null) {
      throw new InvalidRequestException("Verification token is invalid.");
    }

    if (verificationToken.getValidUntil() < Instant.now().toEpochMilli()) {
      throw new InvalidRequestException("Verification token expired, please resend verify email");
    }

    UserInfo userInfo = null;
    try {
      userInfo = getResponse(userClient.completeSignupInvite(verificationToken.getEmail()));
      verificationTokenRepository.delete(verificationToken);

      sendSucceedTelemetryEvent(userInfo.getEmail(), userInfo.getUtmInfo(), userInfo.getDefaultAccountId(), userInfo,
          SignupType.SIGNUP_FORM_FLOW);
      UserInfo finalUserInfo = userInfo;
      executorService.submit(() -> {
        try {
          String url = generateLoginUrl(finalUserInfo.getDefaultAccountId());
          signupNotificationHelper.sendSignupNotification(
              finalUserInfo, EmailType.CONFIRM, PredefinedTemplate.SIGNUP_CONFIRMATION.getIdentifier(), url);
        } catch (URISyntaxException e) {
          log.error("Failed to generate login url", e);
        }
      });

      waitForRbacSetup(userInfo.getDefaultAccountId(), userInfo.getUuid(), userInfo.getEmail());
      log.info("Completed NG signup for {}", userInfo.getEmail());
      return userInfo;
    } catch (Exception e) {
      sendFailedTelemetryEvent(verificationToken.getEmail(), userInfo != null ? userInfo.getUtmInfo() : null, e, null,
          "Complete Signup Invite");
      throw e;
    }
  }

  private void waitForRbacSetup(String accountId, String userId, String email) {
    try {
      boolean rbacSetupSuccessful = busyPollUntilAccountRBACSetupCompletes(accountId, userId, 100, 200);
      if (FALSE.equals(rbacSetupSuccessful)) {
        log.error("User [{}] couldn't be assigned account admin role in stipulated time", email);
        throw new SignupException("Role assignment executes longer than usual, please try logging-in in few minutes");
      }
    } catch (Exception e) {
      log.error(String.format("Failed to check rbac setup for account [%s]", accountId), e);
      throw new SignupException("Role assignment executes longer than usual, please try logging-in in few minutes");
    }
  }

  private boolean busyPollUntilAccountRBACSetupCompletes(
      String accountId, String userId, int maxAttempts, long retryDurationInMillis) {
    RetryConfig config = RetryConfig.custom()
                             .maxAttempts(maxAttempts)
                             .waitDuration(Duration.ofMillis(retryDurationInMillis))
                             .retryOnResult(FALSE::equals)
                             .retryExceptions(Exception.class)
                             .ignoreExceptions(IOException.class)
                             .build();
    Retry retry = Retry.of("check rbac setup", config);
    Retry.EventPublisher publisher = retry.getEventPublisher();
    publisher.onRetry(
        event -> log.info("Retrying check for rbac setup for account {} {}", accountId, event.toString()));
    return Retry
        .decorateSupplier(retry,
            ()
                -> accessControlClient.hasAccess(
                    ResourceScope.of(accountId, null, null), Resource.of("USER", userId), "core_organization_create"))
        .get();
  }

  private AccountDTO createAccount(SignupDTO dto) {
    try {
      return accountService.createAccount(dto);
    } catch (Exception e) {
      sendFailedTelemetryEvent(dto.getEmail(), dto.getUtmInfo(), e, null, "Account creation");
      throw e;
    }
  }

  private void verifyReCaptcha(SignupDTO dto, String captchaToken) {
    try {
      reCaptchaVerifier.verifyInvisibleCaptcha(captchaToken);
    } catch (Exception e) {
      sendFailedTelemetryEvent(dto.getEmail(), dto.getUtmInfo(), e, null, "ReCaptcha verification");
      throw e;
    }
  }

  private void verifySignupDTO(SignupDTO dto) {
    try {
      signupValidator.validateSignup(dto);
    } catch (SignupException | UserAlreadyPresentException e) {
      sendFailedTelemetryEvent(dto.getEmail(), dto.getUtmInfo(), e, null, "Email validation");
      throw e;
    } catch (WeakPasswordException we) {
      sendFailedTelemetryEvent(dto.getEmail(), dto.getUtmInfo(), we, null, "Password validation");
      throw we;
    }
  }

  private SignupVerificationToken generateNewToken(String email) {
    String token = secureRandAlphaNumString(32);
    SignupVerificationToken verificationToken =
        SignupVerificationToken.builder()
            .email(email)
            .validUntil(Instant.now().plus(SIGNUP_TOKEN_VALIDITY_IN_DAYS, ChronoUnit.DAYS).toEpochMilli())
            .token(token)
            .build();
    return verificationTokenRepository.save(verificationToken);
  }

  private String generateVerifyUrl(String accountId, String token, String email) throws URISyntaxException {
    URIBuilder uriBuilder = getNextGenAuthUiURL(accountId);
    String fragment = String.format(VERIFY_URL, token, email);
    uriBuilder.setFragment(fragment);
    return uriBuilder.toString();
  }

  private String generateLoginUrl(String accountId) throws URISyntaxException {
    URIBuilder uriBuilder = getNextGenAuthUiURL(accountId);
    uriBuilder.setFragment(LOGIN_URL);
    return uriBuilder.toString();
  }

  private URIBuilder getNextGenAuthUiURL(String accountId) throws URISyntaxException {
    String baseUrl = accountService.getBaseUrl(accountId);
    URIBuilder uriBuilder = new URIBuilder(baseUrl);
    uriBuilder.setPath(NG_AUTH_UI_PATH_PREFIX);
    return uriBuilder;
  }

  @Override
  public UserInfo oAuthSignup(OAuthSignupDTO dto) {
    if (DeployVariant.isCommunity(deployVersion)) {
      throw new InvalidRequestException("You are not allowed to oauth signup with community edition");
    }

    try {
      signupValidator.validateEmail(dto.getEmail());
    } catch (SignupException | UserAlreadyPresentException e) {
      sendFailedTelemetryEvent(dto.getEmail(), dto.getUtmInfo(), e, null, "Email validation");
      throw e;
    }

    SignupDTO signupDTO = SignupDTO.builder().email(dto.getEmail()).utmInfo(dto.getUtmInfo()).build();
    AccountDTO account = createAccount(signupDTO);
    UserInfo oAuthUser = createOAuthUser(dto, account);

    sendSucceedTelemetryEvent(
        dto.getEmail(), dto.getUtmInfo(), account.getIdentifier(), oAuthUser, SignupType.OAUTH_FLOW);

    executorService.submit(() -> {
      try {
        String url = generateLoginUrl(oAuthUser.getDefaultAccountId());
        signupNotificationHelper.sendSignupNotification(
            oAuthUser, EmailType.CONFIRM, PredefinedTemplate.SIGNUP_CONFIRMATION.getIdentifier(), url);
      } catch (URISyntaxException e) {
        log.error("Failed to generate login url", e);
      }
    });

    waitForRbacSetup(oAuthUser.getDefaultAccountId(), oAuthUser.getUuid(), oAuthUser.getEmail());
    return oAuthUser;
  }

  /**
   * Verify token in non email verification blocking flow
   * @param token
   * @return
   */
  @Override
  public VerifyTokenResponseDTO verifyToken(String token) {
    Optional<SignupVerificationToken> verificationTokenOptional = verificationTokenRepository.findByToken(token);

    if (!verificationTokenOptional.isPresent()) {
      throw new InvalidRequestException("Email token doesn't exist");
    }

    SignupVerificationToken verificationToken = verificationTokenOptional.get();

    if (verificationToken.getUserId() == null) {
      throw new InvalidRequestException("Cannot verify token in non email verification blocking flow");
    }
    getResponse(userClient.changeUserEmailVerified(verificationToken.getUserId()));
    verificationTokenRepository.delete(verificationToken);
    return VerifyTokenResponseDTO.builder().accountIdentifier(verificationToken.getAccountIdentifier()).build();
  }

  @Override
  public void resendVerificationEmail(String email) {
    SignupInviteDTO response = getResponse(userClient.getSignupInvite(email));
    if (response == null) {
      throw new InvalidRequestException(String.format("Email [%s] has not been signed up", email));
    }

    if (response.isCompleted()) {
      throw new InvalidRequestException(String.format("Email [%s] already verified", email));
    }

    if (!response.isCreatedFromNG()) {
      throw new InvalidRequestException(String.format("Invalid email resend request for [%s]", email));
    }

    SignupVerificationToken verificationToken;
    Optional<SignupVerificationToken> verificationTokenOptional = verificationTokenRepository.findByEmail(email);
    String token = secureRandAlphaNumString(32);
    // update with new token or create if not exists
    if (verificationTokenOptional.isPresent()) {
      verificationToken = verificationTokenOptional.get();
      verificationToken.setToken(token);
      verificationToken.setValidUntil(
          Instant.now().plus(SIGNUP_TOKEN_VALIDITY_IN_DAYS, ChronoUnit.DAYS).toEpochMilli());
    } else {
      verificationToken = generateNewToken(email);
    }
    SignupVerificationToken result = verificationTokenRepository.save(verificationToken);

    try {
      String url = generateVerifyUrl(null, result.getToken(), email);
      signupNotificationHelper.sendSignupNotification(
          UserInfo.builder().email(email).defaultAccountId(UNDEFINED_ACCOUNT_ID).build(), EmailType.VERIFY,
          PredefinedTemplate.EMAIL_VERIFY.getIdentifier(), url);
    } catch (URISyntaxException e) {
      throw new InvalidRequestException("Failed to generate verify url", e);
    }
    log.info("Resend verification email for {}", email);
  }

  private UserInfo createUser(SignupDTO signupDTO, AccountDTO account) {
    try {
      String passwordHash = hashpw(signupDTO.getPassword(), BCrypt.gensalt());
      List<AccountDTO> accountList = new ArrayList<>();
      accountList.add(account);

      String name = account.getName();

      UserRequestDTO userRequest = UserRequestDTO.builder()
                                       .email(signupDTO.getEmail())
                                       .name(name)
                                       .passwordHash(passwordHash)
                                       .accountName(account.getName())
                                       .companyName(account.getCompanyName())
                                       .accounts(accountList)
                                       .emailVerified(false)
                                       .defaultAccountId(account.getIdentifier())
                                       .build();
      return getResponse(userClient.createNewUser(userRequest));
    } catch (Exception e) {
      sendFailedTelemetryEvent(signupDTO.getEmail(), signupDTO.getUtmInfo(), e, account, "User creation");
      throw e;
    }
  }

  private void sendFailedTelemetryEvent(
      String email, UtmInfo utmInfo, Exception e, AccountDTO accountDTO, String failedAt) {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put("reason", e.getMessage());
    properties.put("failedAt", failedAt);
    addUtmInfoToProperties(utmInfo, properties);

    if (accountDTO != null) {
      properties.put("company", accountDTO.getCompanyName());
      telemetryReporter.sendTrackEvent(FAILED_EVENT_NAME, email, accountDTO.getIdentifier(), properties,
          ImmutableMap.<Destination, Boolean>builder().put(Destination.SALESFORCE, true).build(), Category.SIGN_UP);
    } else {
      telemetryReporter.sendTrackEvent(FAILED_EVENT_NAME, email, null, properties,
          ImmutableMap.<Destination, Boolean>builder().put(Destination.SALESFORCE, true).build(), Category.SIGN_UP);
    }
  }

  private void sendSucceedTelemetryEvent(
      String email, UtmInfo utmInfo, String accountId, UserInfo userInfo, String source) {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put("email", userInfo.getEmail());
    properties.put("name", userInfo.getName());
    properties.put("id", userInfo.getUuid());
    properties.put("startTime", String.valueOf(Instant.now().toEpochMilli()));
    properties.put("accountId", accountId);
    properties.put("source", source);

    addUtmInfoToProperties(utmInfo, properties);
    telemetryReporter.sendIdentifyEvent(userInfo.getEmail(), properties,
        ImmutableMap.<Destination, Boolean>builder().put(Destination.MARKETO, true).build());
    telemetryReporter.flush();

    // Wait 20 seconds, to ensure identify is sent before track
    ScheduledExecutorService tempExecutor = Executors.newSingleThreadScheduledExecutor();
    tempExecutor.schedule(
        ()
            -> telemetryReporter.sendTrackEvent(SUCCEED_EVENT_NAME, email, accountId, properties,
                ImmutableMap.<Destination, Boolean>builder().put(Destination.MARKETO, true).build(), Category.SIGN_UP),
        20, TimeUnit.SECONDS);
    log.info("Signup telemetry sent");
  }

  private void sendCommunitySucceedTelemetry(String email, String accountId, UserInfo userInfo, String source) {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put("email", userInfo.getEmail());
    properties.put("name", userInfo.getName());
    properties.put("id", userInfo.getUuid());
    properties.put("firstInstallTime", String.valueOf(Instant.now().toEpochMilli()));
    properties.put("accountId", accountId);
    properties.put("source", source);
    try {
      properties.put("hostName", InetAddress.getLocalHost().getHostName());
    } catch (UnknownHostException e) {
      log.error("Unabled to fetch local host name", e);
      properties.put("hostName", "unknown");
    }
    properties.put("version", versionInfoManager.getVersionInfo());

    telemetryReporter.sendIdentifyEvent(
        userInfo.getEmail(), properties, null, TelemetryOption.builder().sendForCommunity(true).build());

    telemetryReporter.sendTrackEvent(SUCCEED_EVENT_NAME, email, accountId, properties, null, Category.SIGN_UP,
        TelemetryOption.builder().sendForCommunity(true).build());
    log.info("Community Signup telemetry sent");
  }

  private void sendSucceedInvite(String email, UtmInfo utmInfo) {
    HashMap<String, Object> properties = new HashMap<>();
    properties.put("email", email);
    properties.put("startTime", String.valueOf(Instant.now().toEpochMilli()));
    addUtmInfoToProperties(utmInfo, properties);
    telemetryReporter.sendIdentifyEvent(
        email, properties, ImmutableMap.<Destination, Boolean>builder().put(Destination.MARKETO, true).build());
    telemetryReporter.flush();

    ScheduledExecutorService tempExecutor = Executors.newSingleThreadScheduledExecutor();
    tempExecutor.schedule(
        ()
            -> telemetryReporter.sendTrackEvent(SUCCEED_SIGNUP_INVITE_NAME, email, UNDEFINED_ACCOUNT_ID, properties,
                ImmutableMap.<Destination, Boolean>builder().put(Destination.MARKETO, true).build(), Category.SIGN_UP),
        20, TimeUnit.SECONDS);
    log.info("Signup invite telemetry sent");
  }

  private void addUtmInfoToProperties(UtmInfo utmInfo, HashMap<String, Object> properties) {
    if (utmInfo != null) {
      properties.put("utm_source", utmInfo.getUtmSource() == null ? "" : utmInfo.getUtmSource());
      properties.put("utm_content", utmInfo.getUtmContent() == null ? "" : utmInfo.getUtmContent());
      properties.put("utm_medium", utmInfo.getUtmMedium() == null ? "" : utmInfo.getUtmMedium());
      properties.put("utm_term", utmInfo.getUtmTerm() == null ? "" : utmInfo.getUtmTerm());
      properties.put("utm_campaign", utmInfo.getUtmCampaign() == null ? "" : utmInfo.getUtmCampaign());
    }
  }

  private UserInfo createOAuthUser(OAuthSignupDTO oAuthSignupDTO, AccountDTO account) {
    try {
      UserRequestDTO userRequest = UserRequestDTO.builder()
                                       .email(oAuthSignupDTO.getEmail())
                                       .name(oAuthSignupDTO.getName())
                                       .accountName(account.getName())
                                       .companyName(account.getCompanyName())
                                       .accounts(Arrays.asList(account))
                                       .emailVerified(true)
                                       .defaultAccountId(account.getIdentifier())
                                       .build();

      return getResponse(userClient.createNewOAuthUser(userRequest));
    } catch (Exception e) {
      sendFailedTelemetryEvent(
          oAuthSignupDTO.getEmail(), oAuthSignupDTO.getUtmInfo(), e, account, "OAuth user creation");
      throw e;
    }
  }
}
