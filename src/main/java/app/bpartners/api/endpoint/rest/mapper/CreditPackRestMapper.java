package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreditPack;
import app.bpartners.api.endpoint.rest.model.CreditPurchaseType;
import app.bpartners.api.model.credit.CreditUnitPrice;
import org.springframework.stereotype.Component;

@Component
public class CreditPackRestMapper {

  public CreditPack toRest(
      app.bpartners.api.model.credit.CreditPack domain, CreditUnitPrice unitPrice) {
    var credits = domain.getCredits();
    return new CreditPack()
        .id(domain.getId())
        .code(domain.getCode().toString())
        .description(domain.getDescription())
        .creditPurchaseType(purchaseTypeToRest(domain.getCreditPurchaseType()))
        .credits(credits)
        .creditUnitPriceInCentsWithoutVat(unitPrice.inCentsWithoutVat())
        .creditUnitPriceInCentsWithVat(unitPrice.inCentsWithVat())
        .priceInCentsWithoutVat(unitPrice.totalInCentsWithoutVat(credits))
        .priceInCentsWithVat(unitPrice.totalInCentsWithVat(credits))
        .vatPercent(unitPrice.vatPercent())
        .validityDays(domain.getValidityDays())
        .isMostChosen(domain.isMostChosen())
        .isDeprecated(domain.isDeprecated())
        .displayPosition(domain.getDisplayPosition());
  }

  private CreditPurchaseType purchaseTypeToRest(
      app.bpartners.api.model.credit.CreditPurchaseType domainPurchaseType) {
    if (domainPurchaseType == null) {
      return null;
    }
    return switch (domainPurchaseType) {
      case PACK -> CreditPurchaseType.PACK;
      case CUSTOM -> CreditPurchaseType.CUSTOM;
    };
  }
}
