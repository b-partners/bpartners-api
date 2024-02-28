package app.bpartners.api.service;

import static app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum.CASH;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static java.time.Instant.now;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.PaymentStatus;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceSummary;
import app.bpartners.api.model.Money;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.InvoiceSummaryRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class InvoiceSummaryService {
  private final InvoiceSummaryRepository repository;
  private final InvoiceRepository invoiceRepository;
  private final UserService userService;

  @Transactional
  public List<InvoiceSummary> updateInvoicesSummary() {
    return userService.findAll().stream()
        .filter(user -> user.getStatus() == EnableStatus.ENABLED)
        .map(user -> updateInvoiceSummary(user.getId()))
        .toList();
  }

  public InvoiceSummary updateInvoiceSummary(String idUser) {
    InvoiceSummary computedSummary = computeInvoiceSummary(idUser);
    return save(computedSummary);
  }

  private InvoiceSummary computeInvoiceSummary(String idUser) {
    List<Invoice> invoices = invoiceRepository.findAllEnabledByIdUser(idUser);
    InvoiceSummary.InvoiceSummaryContent paid = filterPaidInvoicesSummary(invoices);
    InvoiceSummary.InvoiceSummaryContent unpaid = filterUnpaidInvoicesSummary(invoices);
    InvoiceSummary.InvoiceSummaryContent proposal = filterProposalInvoicesSummary(invoices);
    return InvoiceSummary.builder()
        .idUser(idUser)
        .paid(paid)
        .unpaid(unpaid)
        .proposal(proposal)
        .updatedAt(now())
        .build();
  }

  private InvoiceSummary.InvoiceSummaryContent filterPaidInvoicesSummary(List<Invoice> invoices) {
    Stream<Invoice> invoiceStream =
        invoices.parallelStream()
            .filter(
                invoice ->
                    (invoice.getStatus() == PAID
                        || (invoice.getStatus() == CONFIRMED
                            && invoice.getPaymentRegulations().stream()
                                .anyMatch(
                                    payment ->
                                        payment.getPaymentRequest().getStatus()
                                            == PaymentStatus.PAID))));
    Money amount =
        invoiceStream
            .map(
                invoice -> {
                  if (invoice.getPaymentType() == CASH) {
                    return new Money(invoice.getTotalPriceWithVat());
                  } else {
                    return invoice.getPaymentRegulations().stream()
                        .filter(
                            payment ->
                                payment.getPaymentRequest().getStatus() == PaymentStatus.PAID)
                        .map(payment -> new Money(payment.getPaymentRequest().getAmount()))
                        .reduce(Money::add)
                        .orElse(new Money());
                  }
                })
            .reduce(Money::add)
            .orElse(new Money());

    return InvoiceSummary.InvoiceSummaryContent.builder().amount(amount).build();
  }

  private InvoiceSummary.InvoiceSummaryContent filterUnpaidInvoicesSummary(List<Invoice> invoices) {
    Stream<Invoice> invoiceStream =
        invoices.parallelStream().filter(invoice -> invoice.getStatus() == CONFIRMED);
    Money amount =
        invoiceStream
            .flatMap(
                invoice -> {
                  if (invoice.getPaymentType() == CASH) {
                    return Stream.of(new Money(invoice.getTotalPriceWithVat()));
                  } else {
                    return invoice.getPaymentRegulations().stream()
                        .filter(
                            payment ->
                                payment.getPaymentRequest().getStatus() == PaymentStatus.UNPAID)
                        .map(payment -> new Money(payment.getPaymentRequest().getAmount()));
                  }
                })
            .reduce(Money::add)
            .orElse(new Money());

    return InvoiceSummary.InvoiceSummaryContent.builder().amount(amount).build();
  }

  private InvoiceSummary.InvoiceSummaryContent filterProposalInvoicesSummary(
      List<Invoice> invoices) {
    Stream<Invoice> filteredInvoices =
        invoices.parallelStream().filter(invoice -> invoice.getStatus() == PROPOSAL);
    Money amount =
        filteredInvoices
            .flatMap(
                invoice -> {
                  if (invoice.getPaymentType() == CASH) {
                    return Stream.of(new Money(invoice.getTotalPriceWithVat()));
                  } else {
                    return invoice.getPaymentRegulations().stream()
                        .map(payment -> new Money(payment.getPaymentRequest().getAmount()));
                  }
                })
            .reduce(Money::add)
            .orElse(new Money());

    return InvoiceSummary.InvoiceSummaryContent.builder().amount(amount).build();
  }

  public InvoiceSummary save(InvoiceSummary invoiceSummary) {
    return repository.saveAll(List.of(invoiceSummary)).get(0);
  }

  public InvoiceSummary findLatestInvoiceSummary(String idUser) {
    return repository.findTopByIdUser(idUser);
  }

  @Transactional
  public InvoiceSummary getOrComputeLatestSummary(String idUser) {
    InvoiceSummary savedSummary = findLatestInvoiceSummary(idUser);
    return savedSummary == null ? computeInvoiceSummary(idUser) : savedSummary;
  }
}
