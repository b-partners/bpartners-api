package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.InvoiceCrupdated;
import app.bpartners.api.model.exception.NotImplementedException;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class InvoiceCrupdatedService implements Consumer<InvoiceCrupdated> {
  @Override
  public void accept(InvoiceCrupdated invoiceCrupdated) {
    throw new NotImplementedException("Not supported for now");
  }
}
