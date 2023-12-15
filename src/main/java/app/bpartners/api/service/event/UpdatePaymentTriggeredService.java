package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.gen.UpdatePaymentTriggered;
import app.bpartners.api.service.PaymentScheduleService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class UpdatePaymentTriggeredService implements Consumer<UpdatePaymentTriggered> {
  private final PaymentScheduleService service;

  @Override
  public void accept(UpdatePaymentTriggered updatePaymentTriggered) {
    service.updatePaymentStatus();
  }
}
