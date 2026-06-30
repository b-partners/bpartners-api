package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceRequested;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceTriggered;
import app.bpartners.api.endpoint.event.model.UpcomingDebitedCustomerExportRequested;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.subscription.UpcomingUserDebitService;
import java.time.YearMonth;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MonthlySubscriptionInvoiceTriggeredService
    implements Consumer<MonthlySubscriptionInvoiceTriggered> {
  private final UserRepository userRepository;
  private final EventProducer eventProducer;
  private final UserSubscriptionConf userSubscriptionConf;
  private final UpcomingUserDebitService upcomingUserDebitService;

  @Override
  public void accept(MonthlySubscriptionInvoiceTriggered event) {
    var upcomingUserDebited = upcomingUserDebitService.getUpcomingUserDebited();
    var userToCredit = userRepository.getById(userSubscriptionConf.getUserToCreditId());

    if (!upcomingUserDebited.isEmpty()) {
      var actualMonth = YearMonth.now();
      eventProducer.accept(List.of(new UpcomingDebitedCustomerExportRequested(actualMonth)));
    }
    upcomingUserDebited.forEach(
        userToAttemptDebit ->
            eventProducer.accept(
                List.of(
                    MonthlySubscriptionInvoiceRequested.builder()
                        .userToCredit(userToCredit)
                        .userToAttemptDebit(userToAttemptDebit)
                        .build())));
  }
}
