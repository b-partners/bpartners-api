package app.bpartners.api.service;

import static app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum.CASH;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL_CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.PaymentMethod.MULTIPLE;
import static app.bpartners.api.model.Invoice.DEFAULT_TO_PAY_DELAY_DAYS;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.model.PaymentMethod;
import app.bpartners.api.endpoint.rest.model.PaymentStatus;
import app.bpartners.api.model.ArchiveInvoice;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.PaymentHistoryStatus;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.PaymentRequestRepository;
import app.bpartners.api.service.invoice.CustomerInvoiceValidator;
import app.bpartners.api.service.invoice.InvoicePDFProcessor;
import app.bpartners.api.service.invoice.InvoiceValidator;
import app.bpartners.api.service.payment.CreatePaymentRegulationComputing;
import app.bpartners.api.service.payment.PaymentService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class InvoiceService {
  public static final String INVOICE_TEMPLATE = "invoice";
  public static final String DRAFT_TEMPLATE = "draft";
  public static final String DRAFT_REF_PREFIX = "BROUILLON-";
  public static final String PROPOSAL_REF_PREFIX = "DEVIS-";
  private final InvoiceRepository repository;
  private final PaymentInitiationService pis;
  private final PaymentRequestRepository paymentRepository;
  private final InvoicePDFProcessor invoicePDFProcessor;
  private final CreatePaymentRegulationComputing paymentRegulationComputing;
  private final PaymentService paymentService;
  private final InvoiceValidator invoiceValidator;
  private final CustomerInvoiceValidator customerInvoiceValidator;

  @Transactional
  public Invoice updatePaymentStatus(String invoiceId, String paymentId, PaymentMethod method) {
    var invoice = getById(invoiceId);
    var paymentRegulations = invoice.getPaymentRegulations();
    var paymentRequest = paymentService.filterByPaymentId(paymentId, invoiceId, paymentRegulations);
    var isUserUpdated = true;

    PaymentRequest savedPayment =
        paymentRepository.save(
            paymentRequest.toBuilder()
                .invoiceId(invoiceId)
                .status(PaymentStatus.PAID)
                .paymentHistoryStatus(
                    PaymentHistoryStatus.builder()
                        .status(PaymentStatus.PAID)
                        .paymentMethod(method)
                        .updatedAt(Instant.now())
                        .userUpdated(isUserUpdated)
                        .build())
                .build());

    paymentRegulations.forEach(
        payment -> {
          if (payment.getPaymentRequest().getId().equals(savedPayment.getId())) {
            payment.setPaymentRequest(savedPayment);
          }
        });
    if (paymentRegulations.stream()
        .allMatch(payment -> payment.getPaymentRequest().getStatus() == PaymentStatus.PAID)) {
      return crupdateInvoice(invoice.toBuilder().status(PAID).paymentMethod(MULTIPLE).build());
    }

    invoicePDFProcessor.apply(invoice);

    return invoice;
  }

  // TODO: refactor and use EntityManager inside Repository to match dynamically
  // TODO: handle invoice with null title value
  @Transactional
  public List<Invoice> getInvoices(
      String idUser,
      PageFromOne page,
      BoundedPageSize pageSize,
      List<InvoiceStatus> statusList,
      ArchiveStatus archiveStatus,
      String title,
      List<String> filters) {
    if (archiveStatus == null) {
      archiveStatus = ArchiveStatus.ENABLED;
    }
    int pageValue = page != null ? page.getValue() - 1 : 0;
    int pageSizeValue = pageSize != null ? pageSize.getValue() : 30;
    List<String> keywords = new ArrayList<>();
    if (filters != null) {
      keywords.addAll(filters);
    }
    if (title != null) {
      keywords.add(title);
      log.warn(
          "DEPRECATED: query parameter title is still used for filtering invoices."
              + " Use the query parameter filters instead.");
    }
    return repository.findAllByIdUserAndCriteria(
        idUser, statusList, archiveStatus, keywords, pageValue, pageSizeValue);
  }

  public Invoice getById(String invoiceId) {
    return repository.getById(invoiceId);
  }

  @Transactional
  public Invoice duplicateAsDraft(String idInvoice, String reference) {
    var actual = getById(idInvoice);
    var paymentRegulations =
        actual.getPaymentRegulations().stream()
            .map(
                payment -> {
                  PaymentRequest request = payment.getPaymentRequest();
                  return payment.toBuilder()
                      .paymentRequest(
                          request.toBuilder()
                              .id(randomUUID().toString())
                              .externalId(null)
                              .paymentUrl(null)
                              .build())
                      .build();
                })
            .toList();
    var duplicatedInvoice =
        actual.toBuilder()
            .id(String.valueOf(randomUUID()))
            .fileId(String.valueOf(randomUUID()))
            .ref(reference)
            .status(DRAFT)
            .paymentUrl(null)
            .paymentRegulations(paymentRegulations)
            .products(
                new ArrayList<>(actual.getProducts())
                    .stream()
                        .peek(product -> product.setId(String.valueOf(randomUUID())))
                        .collect(Collectors.toList()))
            .build();
    return crupdateInvoice(duplicatedInvoice);
  }

  public List<Invoice> archiveInvoices(List<ArchiveInvoice> archiveInvoices) {
    return repository.saveAll(archiveInvoices);
  }

  @Transactional
  public Invoice crupdateInvoice(Invoice newInvoice) {
    invoiceValidator.checkReferenceAvailability(newInvoice);
    customerInvoiceValidator.accept(newInvoice);

    if (!newInvoice.getActualHolder().isSubjectToVat()) {
      newInvoice.getProducts().forEach(product -> product.setVatPercent(new Fraction()));
    }

    var invoiceBuilder = newInvoice.toBuilder();
    invoiceBuilder.paymentRegulations(paymentRegulationComputing.computeWithoutPisURL(newInvoice));
    repository
        .pwFindOptionalById(newInvoice.getId())
        .ifPresent(
            oldInvoice -> {
              invoiceBuilder.fileId(oldInvoice.getFileId());
              handleStatusChange(newInvoice, oldInvoice, invoiceBuilder);
            });
    var actual = invoiceBuilder.build();

    return repository.crupdate(actual);
  }

  private void handleStatusChange(
      Invoice newInvoice, Invoice oldInvoice, Invoice.InvoiceBuilder invoiceBuilder) {
    InvoiceStatus newStatus = newInvoice.getStatus();
    InvoiceStatus oldStatus = oldInvoice.getStatus();
    switch (newStatus) {
      case PROPOSAL_CONFIRMED -> handlePaymentType(newInvoice, oldInvoice, invoiceBuilder);
      case CONFIRMED -> {
        handlePaymentType(newInvoice, oldInvoice, invoiceBuilder);
        if (oldStatus == PROPOSAL) {
          invoiceBuilder.status(PROPOSAL_CONFIRMED);
        }
      }
      case PAID -> {
        if (oldStatus == CONFIRMED) {
          invoiceBuilder.validityDate(null);
          invoiceBuilder.sendingDate(oldInvoice.getSendingDate());

          if (newInvoice.getPaymentType() == CASH) {
            invoiceBuilder.paymentUrl(oldInvoice.getPaymentUrl());
            invoiceBuilder.toPayAt(
                newInvoice.getSendingDate().plusDays(newInvoice.getDelayInPaymentAllowed()));
          } else {
            invoiceBuilder.paymentRegulations(oldInvoice.getPaymentRegulations());
          }
        }
      }
    }
  }

  private void handlePaymentType(
      Invoice newInvoice, Invoice oldInvoice, Invoice.InvoiceBuilder invoiceBuilder) {
    var paymentType = newInvoice.getPaymentType();
    if (paymentType.equals(CASH)) {
      var delayInPaymentAllowed =
          newInvoice.getDelayInPaymentAllowed() == null
              ? DEFAULT_TO_PAY_DELAY_DAYS
              : newInvoice.getDelayInPaymentAllowed();
      invoiceBuilder.toPayAt(newInvoice.getSendingDate().plusDays(delayInPaymentAllowed));
      invoiceBuilder.paymentUrl(
          newInvoice.getTotalPriceWithVat().getCentsAsDecimal() != 0
              ? pis.initiateInvoicePayment(newInvoice).getRedirectUrl()
              : newInvoice.getPaymentUrl());
    } else {
      var oldAmount = getPaymentsAmount(oldInvoice);
      var newAmount = getPaymentsAmount(newInvoice);
      var oldPercentValue = getPaymentPercent(oldInvoice);
      var newPercentValue = getPaymentPercent(newInvoice);
      if (newAmount != oldAmount && newPercentValue != oldPercentValue) {
        invoiceBuilder.paymentRegulations(paymentRegulationComputing.computeWithPisURL(newInvoice));
        invoiceBuilder.paymentUrl(null);
      }
    }
  }

  private double getPaymentsAmount(Invoice invoice) {
    return invoice.getSortedMultiplePayments().stream()
        .map(CreatePaymentRegulation::getPaymentRequest)
        .map(PaymentRequest::getAmount)
        .map(Fraction::getApproximatedValue)
        .reduce(0.0, Double::sum);
  }

  private double getPaymentPercent(Invoice invoice) {
    return invoice.getSortedMultiplePayments().stream()
        .map(CreatePaymentRegulation::getPercent)
        .map(Fraction::getApproximatedValue)
        .reduce(0.0, Double::sum);
  }
}
