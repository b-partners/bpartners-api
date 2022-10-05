package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.Marketplace;
import org.springframework.stereotype.Component;

@Component
public class MarketplaceRestMapper {
  public Marketplace toRest(app.bpartners.api.model.Marketplace domain) {
    return new Marketplace()
        .id(domain.getId())
        .name(domain.getName())
        .description(domain.getDescription())
        .phoneNumber(domain.getPhoneNumber())
        .logoUrl(domain.getLogoUrl())
        .websiteUrl(domain.getWebsiteUrl());
  }
}
