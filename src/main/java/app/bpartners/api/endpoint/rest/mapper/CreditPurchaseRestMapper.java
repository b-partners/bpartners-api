package app.bpartners.api.endpoint.rest.mapper;

import static app.bpartners.api.model.credit.CreditPurchaseType.CUSTOM;
import static app.bpartners.api.model.credit.CreditPurchaseType.PACK;

import app.bpartners.api.endpoint.rest.model.CreditPackPurchase;
import app.bpartners.api.endpoint.rest.model.CreditPurchase;
import app.bpartners.api.endpoint.rest.model.CreditPurchaseOrigin;
import app.bpartners.api.endpoint.rest.model.CreditPurchaseStatus;
import app.bpartners.api.endpoint.rest.model.CreditPurchaseType;
import app.bpartners.api.endpoint.rest.model.CustomCreditPurchase;
import app.bpartners.api.endpoint.rest.model.Redirection1;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreditPurchaseRestMapper {
  private final CreditPackRestMapper creditPackRestMapper;

  public CreditPurchase toRest(app.bpartners.api.model.credit.CreditPurchase domain) {
    return new CreditPurchase()
        .id(domain.getId())
        .type(typeToRest(domain.getType()))
        .packPurchase(packPurchaseToRest(domain))
        .customPurchase(customPurchaseToRest(domain))
        .credits(domain.getCredits())
        .amountInCentsWithoutVat(domain.getAmountInCentsWithoutVat())
        .amountInCentsWithVat(domain.getAmountInCentsWithVat())
        .vatPercent(domain.getVatPercent())
        .status(statusToRest(domain.getStatus()))
        .origin(originToRest(domain.getOrigin()))
        .redirection(redirectionToRest(domain))
        .creditTransactionId(domain.getCreditTransactionId())
        .invoiceId(domain.getInvoiceId())
        .creationDatetime(domain.getCreationDatetime())
        .completionDatetime(domain.getCompletionDatetime())
        .creditsExpirationDatetime(domain.getCreditsExpirationDatetime());
  }

  public app.bpartners.api.model.credit.CreditPurchaseStatus toDomainStatus(
      CreditPurchaseStatus restStatus) {
    return restStatus == null
        ? null
        : app.bpartners.api.model.credit.CreditPurchaseStatus.valueOf(restStatus.name());
  }

  private CreditPackPurchase packPurchaseToRest(
      app.bpartners.api.model.credit.CreditPurchase domain) {
    if (!PACK.equals(domain.getType()) || domain.getCreditPack() == null) {
      return null;
    }
    return new CreditPackPurchase()
        .creditPack(
            creditPackRestMapper.toRestDescription(
                domain.getCreditPack(), domain.unitPriceApplied()))
        .quantity(domain.getQuantity());
  }

  private CustomCreditPurchase customPurchaseToRest(
      app.bpartners.api.model.credit.CreditPurchase domain) {
    return CUSTOM.equals(domain.getType())
        ? new CustomCreditPurchase()
            .creditUnitPriceInCentsWithoutVat(domain.getCreditUnitPriceInCentsWithoutVat())
        : null;
  }

  private Redirection1 redirectionToRest(app.bpartners.api.model.credit.CreditPurchase domain) {
    var redirectionUrl = domain.getRedirectionUrl();
    var successUrl = domain.getRedirectionSuccessUrl();
    var failureUrl = domain.getRedirectionFailureUrl();
    if (redirectionUrl == null && successUrl == null && failureUrl == null) {
      return null;
    }
    return new Redirection1()
        .redirectionUrl(redirectionUrl)
        .redirectionStatusUrls(
            successUrl == null && failureUrl == null
                ? null
                : new RedirectionStatusUrls().successUrl(successUrl).failureUrl(failureUrl));
  }

  private CreditPurchaseType typeToRest(
      app.bpartners.api.model.credit.CreditPurchaseType domainType) {
    return domainType == null ? null : CreditPurchaseType.valueOf(domainType.name());
  }

  private CreditPurchaseStatus statusToRest(
      app.bpartners.api.model.credit.CreditPurchaseStatus domainStatus) {
    return domainStatus == null ? null : CreditPurchaseStatus.valueOf(domainStatus.name());
  }

  private CreditPurchaseOrigin originToRest(
      app.bpartners.api.model.credit.CreditPurchaseOrigin domainOrigin) {
    return domainOrigin == null ? null : CreditPurchaseOrigin.valueOf(domainOrigin.name());
  }
}
