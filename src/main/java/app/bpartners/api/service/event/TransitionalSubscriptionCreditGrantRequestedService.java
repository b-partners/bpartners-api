package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.TransitionalSubscriptionCreditGrantRequested;
import app.bpartners.api.service.credit.CreditGrantService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransitionalSubscriptionCreditGrantRequestedService
    implements Consumer<TransitionalSubscriptionCreditGrantRequested> {
  private static final long TRANSITIONAL_SUBSCRIPTION_CREDITS = 25L;
  private final CreditGrantService creditGrantService;

  @Override
  public void accept(TransitionalSubscriptionCreditGrantRequested event) {
    creditGrantService.grantTransitionalCredits(
        event.getUserId(), TRANSITIONAL_SUBSCRIPTION_CREDITS);
  }
}
