package io.harness.resourcegroup.framework.repositories.custom;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.resourcegroup.model.ResourceGroupV2;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PL)
public interface ResourceGroupV2RepositoryCustom {
  Page<ResourceGroupV2> findAll(Criteria criteria, Pageable pageable);
  Optional<ResourceGroupV2> find(Criteria criteria);
  boolean delete(Criteria criteria);
  boolean updateMultiple(Query query, Update update);
}
