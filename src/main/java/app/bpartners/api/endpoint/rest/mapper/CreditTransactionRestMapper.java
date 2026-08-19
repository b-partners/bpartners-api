package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreditTransaction;
import app.bpartners.api.endpoint.rest.model.CreditTransactionMovementType;
import app.bpartners.api.endpoint.rest.model.CreditTransactionType;
import org.springframework.stereotype.Component;

@Component
public class CreditTransactionRestMapper {

  public CreditTransaction toRest(app.bpartners.api.model.credit.CreditTransaction domain) {
    return new CreditTransaction()
        .id(domain.getId())
        .type(typeToRest(domain.getType()))
        .movementType(movementTypeToRest(domain.getMovementType()))
        .credits(domain.getCredits())
        .expirationDatetime(domain.getExpirationDatetime())
        .creationDatetime(domain.getCreationDatetime());
  }

  public app.bpartners.api.model.credit.CreditTransactionType toDomainType(
      CreditTransactionType restType) {
    return restType == null
        ? null
        : app.bpartners.api.model.credit.CreditTransactionType.valueOf(restType.name());
  }

  private CreditTransactionType typeToRest(
      app.bpartners.api.model.credit.CreditTransactionType domainType) {
    return domainType == null ? null : CreditTransactionType.valueOf(domainType.name());
  }

  private CreditTransactionMovementType movementTypeToRest(
      app.bpartners.api.model.credit.CreditTransactionMovementType domainMovementType) {
    return domainMovementType == null
        ? null
        : CreditTransactionMovementType.valueOf(domainMovementType.name());
  }
}
