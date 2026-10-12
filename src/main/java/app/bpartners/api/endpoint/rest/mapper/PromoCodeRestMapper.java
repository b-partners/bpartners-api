package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.PromoCode;
import app.bpartners.api.endpoint.rest.model.PromoCodeStats;
import app.bpartners.api.endpoint.rest.model.PromoCodeType;
import org.springframework.stereotype.Component;

@Component
public class PromoCodeRestMapper {
  public PromoCode toRest(app.bpartners.api.model.PromoCode domain) {
    return new PromoCode()
        .code(domain.getCode())
        .type(PromoCodeType.valueOf(domain.getType().name()))
        .label(domain.getLabel());
  }

  public PromoCodeStats toRest(app.bpartners.api.model.PromoCodeStats domain) {
    return new PromoCodeStats()
        .promoCode(toRest(domain.getPromoCode()))
        .registeredCount(domain.getRegisteredCount());
  }
}
