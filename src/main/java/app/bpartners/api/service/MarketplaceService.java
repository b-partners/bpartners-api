package app.bpartners.api.service;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Marketplace;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.repository.MarketplaceRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MarketplaceService {
  private final MarketplaceRepository repository;

  public List<Marketplace> getMarketplacesByAccountId(
      String accountId, PageFromOne page, BoundedPageSize pageSize) {
    int pageValue = page.getValue() - 1;
    int pageSizeValue = pageSize.getValue();
    return repository.findAllByAccountId(accountId, pageValue, pageSizeValue);
  }
}
