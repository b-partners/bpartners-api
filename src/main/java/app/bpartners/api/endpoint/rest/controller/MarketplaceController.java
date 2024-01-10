package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.MarketplaceRestMapper;
import app.bpartners.api.endpoint.rest.model.Marketplace;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.service.MarketplaceService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class MarketplaceController {
  private final MarketplaceService service;
  private final MarketplaceRestMapper mapper;

  @GetMapping("/accounts/{id}/marketplaces")
  public List<Marketplace> getMarketplaces(
      @PathVariable("id") String accountId,
      @RequestParam(value = "page", required = false) PageFromOne page,
      @RequestParam(value = "pageSize", required = false) BoundedPageSize pageSize) {
    return service.getMarketplacesByAccountId(accountId, page, pageSize).stream()
        .map(mapper::toRest)
        .toList();
  }
}
