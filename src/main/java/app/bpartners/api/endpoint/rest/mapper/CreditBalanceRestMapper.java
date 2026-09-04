package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreditBalance;
import app.bpartners.api.endpoint.rest.model.CreditExpiration;
import app.bpartners.api.endpoint.rest.model.CreditOrigin;
import org.springframework.stereotype.Component;

@Component
public class CreditBalanceRestMapper {

  public CreditBalance toRest(app.bpartners.api.model.credit.CreditBalance domain) {
    return new CreditBalance()
        .spendableCredits(domain.getSpendableCredits())
        .grantedCredits(domain.getGrantedCredits())
        .purchasedCredits(domain.getPurchasedCredits())
        .creditCostPerAnalysis(domain.getCreditCostPerAnalysis())
        .estimatedRemainingAnalyses(domain.getEstimatedRemainingAnalyses())
        .nextGrantDatetime(domain.getNextGrantDatetime())
        .expirations(domain.getExpirations().stream().map(this::expirationToRest).toList())
        .updatedAt(domain.getUpdatedAt());
  }

  private CreditExpiration expirationToRest(
      app.bpartners.api.model.credit.CreditExpiration domain) {
    return new CreditExpiration()
        .credits(domain.getCredits())
        .expirationDatetime(domain.getExpirationDatetime())
        .origin(originToRest(domain.getOrigin()));
  }

  private CreditOrigin originToRest(app.bpartners.api.model.credit.CreditOrigin domainOrigin) {
    if (domainOrigin == null) {
      return null;
    }
    return switch (domainOrigin) {
      case SUBSCRIPTION_GRANT -> CreditOrigin.SUBSCRIPTION_GRANT;
      case PURCHASE -> CreditOrigin.PURCHASE;
      case ADJUSTMENT -> CreditOrigin.ADJUSTMENT;
    };
  }
}
