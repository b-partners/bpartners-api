package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Marketplace;
import app.bpartners.api.model.mapper.MarketplaceMapper;
import app.bpartners.api.repository.MarketplaceRepository;
import app.bpartners.api.repository.jpa.MarketplaceJpaRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class MarketplaceRepositoryImpl implements MarketplaceRepository {
  private final MarketplaceJpaRepository jpaRepository;
  private final MarketplaceMapper mapper;

  @Override
  public List<Marketplace> findAllByAccountId(String accountId, int page, int pageSize) {
    Pageable pageable = PageRequest.of(page, pageSize);
    //TODO: change to findAllByAccountId when it's correctly set but for now return all marketplaces
    return jpaRepository.findAll(pageable).stream().map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }
}
