package app.bpartners.api.service.invoice;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.InvoiceExportRequested;
import app.bpartners.api.model.InvoiceExportRequest;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.InvoiceExportRequestRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceExportRequestService {
  public static final int DEFAULT_BATCH_SIZE = 500;
  private final InvoiceExportRequestRepository repository;
  private final InvoiceRepository invoiceRepository;
  private final EventProducer<InvoiceExportRequested> eventProducer;

  public InvoiceExportRequest getById(String requestIdentifier) {
    return repository
        .findById(requestIdentifier)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "InvoiceExportRequest.id=" + requestIdentifier + " not found"));
  }

  @Transactional
  public List<InvoiceExportRequest> createInvoiceExportRequestList(
      List<InvoiceExportRequest> invoiceExportRequestList) {
    var invoiceExportRequestsWithComputedBatch =
        invoiceExportRequestList.stream().map(this::withBatchPlan).toList();

    var savedInvoiceExportRequests = repository.saveAll(invoiceExportRequestsWithComputedBatch);

    var events =
        savedInvoiceExportRequests.stream()
            .filter(request -> request.getTotalBatchCount() > 0)
            .map(
                request -> {
                  var firstPage = 0;
                  return InvoiceExportRequested.builder()
                      .invoiceExportRequestId(request.getId())
                      .page(firstPage)
                      .build();
                })
            .toList();

    if (!events.isEmpty()) {
      eventProducer.accept(events);
    }

    return savedInvoiceExportRequests;
  }

  private InvoiceExportRequest withBatchPlan(InvoiceExportRequest request) {
    var from = request.getFrom();
    var to = request.getTo();
    var batchSize = request.getBatchSize();

    var totalInvoiceCount =
        invoiceRepository.countAllByIdUserAndSendingDateBetweenAndCriteria(
            request.getUserId(), from, to, request.getStatusList(), request.getArchiveStatus());

    return request.toBuilder()
        .totalInvoiceCount(totalInvoiceCount)
        .totalBatchCount((int) Math.ceil((double) totalInvoiceCount / batchSize))
        .batchList(new ArrayList<>())
        .build();
  }
}
