package app.bpartners.api.service.event;

import static java.time.Instant.now;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.SubscriptionRemainingPeriodInvoiceRequested;
import app.bpartners.api.endpoint.event.model.SubscriptionRemainingPeriodInvoiceTriggered;
import app.bpartners.api.repository.UserSubscriptionCommitmentJpaRepository;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionRemainingPeriodInvoiceTriggeredService
    implements Consumer<SubscriptionRemainingPeriodInvoiceTriggered> {
  private final UserSubscriptionCommitmentJpaRepository userSubscriptionCommitmentRepository;
  private final EventProducer eventProducer;

  @Override
  public void accept(SubscriptionRemainingPeriodInvoiceTriggered event) {
    var userIdentifiers =
        userSubscriptionCommitmentRepository.findUserIdsWithCommitmentEndingAfter(now());
    log.info(
        "Remaining commitment period invoicing triggered, fanning out for {} committed user(s)",
        userIdentifiers.size());
    if (userIdentifiers.isEmpty()) {
      return;
    }
    eventProducer.accept(
        userIdentifiers.stream()
            .map(
                userIdentifier ->
                    SubscriptionRemainingPeriodInvoiceRequested.builder()
                        .userId(userIdentifier)
                        .build())
            .toList());
  }
}
