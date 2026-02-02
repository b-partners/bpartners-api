package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceRequested;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceTriggered;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.UserRepository;
import java.util.HashMap;
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

  @Override
  public void accept(MonthlySubscriptionInvoiceTriggered event) {
    var criteria = new HashMap<String, Object>();
    criteria.put("status", ENABLED);
    var enabledUsers = userRepository.findAllByCriteria(criteria);
    var userToCredit = userRepository.getById(userSubscriptionConf.getUserToCreditId());

    enabledUsers.forEach(
        userToAttemptDebit -> {
          eventProducer.accept(
              List.of(
                  MonthlySubscriptionInvoiceRequested.builder()
                      .userToCredit(userToCredit)
                      .userToAttemptDebit(userToAttemptDebit)
                      .build()));
        });
  }
}
