package app.bpartners.api.service.invoice;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.InvoiceExportLinkRequested;
import app.bpartners.api.model.InvoiceExportRequest;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.jpa.InvoiceExportRequestRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvoiceExportRequestService {
  private final InvoiceExportRequestRepository repository;
  private final EventProducer eventProducer;

  public InvoiceExportRequest getById(String requestIdentifier) {
    return repository
        .findById(requestIdentifier)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "InvoiceExportRequest.id=" + requestIdentifier + " not found"));
  }

  public List<InvoiceExportRequest> submitRequests(List<InvoiceExportRequest> requests) {
    var savedRequests = repository.saveAll(requests);

    savedRequests.forEach(
        invoiceExportRequest -> {
          eventProducer.accept(
              List.of(
                  InvoiceExportLinkRequested.builder()
                      .invoiceExportRequestIdentifier(invoiceExportRequest.getId())
                      .userId(invoiceExportRequest.getUserId())
                      .page(0)
                      .totalPage(0) // TODO
                      .providedArchiveStatus(invoiceExportRequest.getArchiveStatus())
                      .providedStatuses(invoiceExportRequest.getStatusList())
                      .providedFrom(invoiceExportRequest.getFrom())
                      .providedTo(invoiceExportRequest.getTo())
                      .batchSize(invoiceExportRequest.getBatchSize())
                      .build()));
        });
    return savedRequests;
  }
}
