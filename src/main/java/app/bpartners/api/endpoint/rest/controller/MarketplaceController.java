package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.MarketplaceRestMapper;
import app.bpartners.api.endpoint.rest.model.Marketplace;
import app.bpartners.api.service.MarketplaceService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class MarketplaceController {
  private final MarketplaceService service;
  private final MarketplaceRestMapper mapper;

  @GetMapping("/accounts/{id}/marketplaces")
  public List<Marketplace> getMarketplaces(@PathVariable("id") String accountId) {
    return service.getMarketplacesByAccountId(accountId).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }
}
