package app.bpartners.api.repository;

import app.bpartners.api.model.Marketplace;
import java.util.List;

public interface MarketplaceRepository {
  List<Marketplace> findAllByAccountId(String accountId, int page, int pageSize);
}
