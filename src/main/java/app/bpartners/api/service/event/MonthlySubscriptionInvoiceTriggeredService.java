package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceRequested;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceTriggered;
import app.bpartners.api.repository.UserRepository;
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

  @Override
  public void accept(MonthlySubscriptionInvoiceTriggered event) {
    var totalUserCount = userRepository.countUsersByStatus(ENABLED).intValue();
    var userPageCount = (totalUserCount + MAX_SIZE - 1) / MAX_SIZE;
    for (int i = 1; i <= userPageCount; i++) {
      eventProducer.accept(
          List.of(MonthlySubscriptionInvoiceRequested.builder().userPage(i).build()));
    }
  }
}
