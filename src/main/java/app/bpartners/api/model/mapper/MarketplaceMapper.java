package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Marketplace;
import app.bpartners.api.repository.jpa.model.HMarketplace;
import org.springframework.stereotype.Component;

@Component
public class MarketplaceMapper {
  public Marketplace toDomain(HMarketplace entity) {
    return Marketplace.builder()
        .id(entity.getId())
        .phoneNumber(entity.getPhoneNumber())
        .name(entity.getName())
        .description(entity.getDescription())
        .websiteUrl(entity.getWebsiteUrl())
        .logoUrl(entity.getLogoUrl())
        .build();
  }
}