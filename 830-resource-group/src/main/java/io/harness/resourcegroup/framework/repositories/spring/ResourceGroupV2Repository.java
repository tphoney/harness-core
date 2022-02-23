package io.harness.resourcegroup.framework.repositories.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.resourcegroup.framework.repositories.custom.ResourceGroupV2RepositoryCustom;
import io.harness.resourcegroup.model.ResourceGroupV2;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PL)
public interface ResourceGroupV2Repository
    extends PagingAndSortingRepository<ResourceGroupV2, String>, ResourceGroupV2RepositoryCustom {
  List<ResourceGroupV2> deleteByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier);
  Optional<ResourceGroupV2> findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
  Optional<ResourceGroupV2> findByAccountIdentifierAndOrgIdentifierAndProjectIdentifierAndIdentifierAndHarnessManaged(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier,
      boolean harnessManaged);
}