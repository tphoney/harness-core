package io.harness.resourcegroup.framework.repositories.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.resourcegroup.framework.repositories.custom.ResourceGroupV2RepositoryCustom;
import io.harness.resourcegroup.model.ResourceGroupV2;

import java.util.List;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PL)
public interface ResourceGroupV2Repository
    extends PagingAndSortingRepository<ResourceGroupV2, String>, ResourceGroupV2RepositoryCustom {
  List<ResourceGroupV2> deleteByAccountIdentifierAndOrgIdentifierAndProjectIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier);
}
