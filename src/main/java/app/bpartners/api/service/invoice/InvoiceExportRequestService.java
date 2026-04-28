package app.bpartners.api.service.invoice;

import app.bpartners.api.model.InvoiceExportRequest;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.jpa.InvoiceExportRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvoiceExportRequestService {
  private final InvoiceExportRequestRepository repository;

  public InvoiceExportRequest getById(String requestIdentifier) {
    return repository
        .findById(requestIdentifier)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "InvoiceExportRequest.id=" + requestIdentifier + " not found"));
  }
}
