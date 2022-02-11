package io.harness.gitsync.common.helper;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.BHAVYA;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.gitsync.GitSyncTestBase;
import io.harness.ng.userprofile.commons.GithubSCMDTO;
import io.harness.ng.userprofile.commons.SourceCodeManagerDTO;
import io.harness.ng.userprofile.services.api.SourceCodeManagerService;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.Collections;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(PL)
public class UserProfileHelperTest extends GitSyncTestBase {
  private final String ACCOUNT_ID = "ACCOUNT_ID";
  private final String USER_ID = "userId";
  private final String NAME = "name";
  private SourceCodeManagerDTO sourceCodeManagerDTO;
  @Mock SourceCodeManagerService sourceCodeManagerService;
  @Inject UserProfileHelper userProfileHelper;

  @Before
  public void setup() throws Exception {
    MockitoAnnotations.initMocks(this);
    sourceCodeManagerDTO =
        GithubSCMDTO.builder().userIdentifier(USER_ID).name(NAME).accountIdentifier(ACCOUNT_ID).build();
    FieldUtils.writeField(userProfileHelper, "sourceCodeManagerService", sourceCodeManagerService, true);
  }

  @Test
  @Owner(developers = BHAVYA)
  @Category(UnitTests.class)
  public void testValidateIfScmUserProfileIsSet_whenScmNotSet() {
    when(sourceCodeManagerService.get(any())).thenReturn(Collections.EMPTY_LIST);

    assertThatThrownBy(() -> userProfileHelper.validateIfScmUserProfileIsSet(ACCOUNT_ID))
        .isInstanceOf(InvalidRequestException.class)
        .matches(ex
            -> ex.getMessage().equals("We don’t have your git credentials for the selected folder."
                + " Please update the credentials in user profile."));
  }
}
