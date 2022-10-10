package app.bpartners.api.service;

import app.bpartners.api.model.Marketplace;
import app.bpartners.api.repository.MarketplaceRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MarketplaceService {
  private final MarketplaceRepository repository;

  public List<Marketplace> getMarketplacesByAccountId(String accountId) {
    return repository.findAllByAccountId(accountId);
  }
}
