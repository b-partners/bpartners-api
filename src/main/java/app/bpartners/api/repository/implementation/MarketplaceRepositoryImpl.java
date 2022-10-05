package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Marketplace;
import app.bpartners.api.model.mapper.MarketplaceMapper;
import app.bpartners.api.repository.MarketplaceRepository;
import app.bpartners.api.repository.jpa.MarketplaceJpaRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class MarketplaceRepositoryImpl implements MarketplaceRepository {
  private final MarketplaceJpaRepository jpaRepository;
  private final MarketplaceMapper mapper;

  @Override
  public List<Marketplace> findAllByAccountId(String accountId) {
    return jpaRepository.findAllByAccountId(accountId).stream().map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }
}
