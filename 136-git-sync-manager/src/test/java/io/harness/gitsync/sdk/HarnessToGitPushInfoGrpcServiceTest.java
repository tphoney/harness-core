package io.harness.gitsync.sdk;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.BHAVYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.gitsync.FileInfo;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.gitsync.Principal;
import io.harness.gitsync.UserPrincipal;
import io.harness.manage.GlobalContextManager;
import io.harness.rule.Owner;
import io.harness.security.SecurityContextBuilder;
import io.harness.security.SourcePrincipalContextBuilder;
import io.harness.security.dto.PrincipalType;

import com.google.inject.Inject;
import com.google.protobuf.StringValue;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

@OwnedBy(PL)
public class HarnessToGitPushInfoGrpcServiceTest extends GitSyncTestBase {
  private final String accountId = "accountId";
  private final String email = "email";
  private final String userName = "userName";
  private final String name = "name";
  private FileInfo fileInfo;
  @Inject HarnessToGitPushInfoGrpcService harnessToGitPushInfoGrpcService;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void testSetPrincipal() {
    UserPrincipal userPrincipal = UserPrincipal.newBuilder()
                                      .setUserId(StringValue.of(name))
                                      .setUserName(StringValue.of(userName))
                                      .setEmail(StringValue.of(email))
                                      .build();
    Principal principal = Principal.newBuilder().setUserPrincipal(userPrincipal).build();
    FileInfo fileInfo = buildFileInfo(principal);
    try (GlobalContextManager.GlobalContextGuard guard = GlobalContextManager.ensureGlobalContextGuard()) {
      harnessToGitPushInfoGrpcService.setPrincipal(fileInfo);
      assertThat(SecurityContextBuilder.getPrincipal().getType()).isEqualTo(PrincipalType.USER);
      assertThat(SourcePrincipalContextBuilder.getSourcePrincipal().getName()).isEqualTo(name);
    }
  }

  private FileInfo buildFileInfo(Principal principal) {
    fileInfo = FileInfo.newBuilder().setAccountId(accountId).setPrincipal(principal).build();
    return fileInfo;
  }
}
