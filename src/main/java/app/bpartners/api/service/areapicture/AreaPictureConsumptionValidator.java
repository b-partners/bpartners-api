package app.bpartners.api.service.areapicture;

import static app.bpartners.api.model.WhiteListScope.CREDIT_ANALYSIS_NOT_REQUIRED;
import static app.bpartners.api.model.WhiteListScope.SUBSCRIPTION_VALIDATION_NOT_REQUIRED;

import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.exception.InsufficientCreditsException;
import app.bpartners.api.repository.jpa.UserWhiteListedJpaRepository;
import app.bpartners.api.service.credit.CreditService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AreaPictureConsumptionValidator implements Consumer<AreaPicture> {
  private final CreditService creditService;
  private final UserWhiteListedJpaRepository userWhiteListedRepository;

  @Override
  public void accept(AreaPicture areaPicture) {
    var userId = areaPicture.getIdUser();
    if (isExemptedFromCreditAnalysis(userId)) {
      return;
    }
    var creditBalance = creditService.getCreditBalance(userId);
    var spendableCredits = creditBalance.getSpendableCredits();
    var creditCostPerAnalysis = creditBalance.getCreditCostPerAnalysis();
    if (spendableCredits < creditCostPerAnalysis) {
      throw new InsufficientCreditsException(creditCostPerAnalysis, spendableCredits);
    }
  }

  private boolean isExemptedFromCreditAnalysis(String userId) {
    return userWhiteListedRepository
        .findByUserId(userId)
        .filter(
            userWhiteListed ->
                userWhiteListed.getScopes().contains(CREDIT_ANALYSIS_NOT_REQUIRED)
                    || userWhiteListed.getScopes().contains(SUBSCRIPTION_VALIDATION_NOT_REQUIRED))
        .isPresent();
  }
}
