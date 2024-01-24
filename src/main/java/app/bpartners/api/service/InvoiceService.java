package app.bpartners.api.service;

import static app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum.CASH;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL_CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.PaymentMethod.MULTIPLE;
import static app.bpartners.api.model.Invoice.DEFAULT_TO_PAY_DELAY_DAYS;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.utils.PaymentUtils.computeTotalPriceFromPaymentReq;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.model.PaymentMethod;
import app.bpartners.api.endpoint.rest.model.PaymentStatus;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.ArchiveInvoice;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoicesSummary;
import app.bpartners.api.model.Money;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.PaymentHistoryStatus;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.PaymentRequestRepository;
import app.bpartners.api.repository.implementation.InvoiceRepositoryImpl;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apfloat.Aprational;
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
  private final InvoiceRepositoryImpl repositoryImpl;
  private final CustomerService customerService;
  private final PaymentInitiationService pis;
  private final PaymentRequestMapper requestMapper;
  private final PaymentRequestRepository paymentRepository;

  public InvoicesSummary getInvoicesSummary(String idUser) {
    List<Invoice> invoices = repository.findAllByIdUser(idUser);
    InvoicesSummary.InvoiceSummaryContent paid = filterPaidInvoicesSummary(invoices);
    InvoicesSummary.InvoiceSummaryContent unpaid = filterUnpaidInvoicesSummary(invoices);
    InvoicesSummary.InvoiceSummaryContent proposal = filterProposalInvoicesSummary(invoices);
    return InvoicesSummary.builder().paid(paid).unpaid(unpaid).proposal(proposal).build();
  }

  private InvoicesSummary.InvoiceSummaryContent filterPaidInvoicesSummary(List<Invoice> invoices) {
    int count = 0;
    Money amount = new Money();
    List<Invoice> filteredInvoices =
        invoices.stream()
            .filter(
                invoice ->
                    invoice.getArchiveStatus() == ArchiveStatus.ENABLED
                        && (invoice.getStatus() == PAID
                            || (invoice.getStatus() == CONFIRMED
                                && invoice.getPaymentRegulations().stream()
                                    .anyMatch(
                                        payment ->
                                            payment.getPaymentRequest().getStatus()
                                                == PaymentStatus.PAID))))
            .toList();
    for (Invoice i : filteredInvoices) {
      if (i.getPaymentType() == CASH) {
        amount = amount.add(new Money(i.getTotalPriceWithVat()));
      } else {
        for (var payment : i.getPaymentRegulations()) {
          if (payment.getPaymentRequest().getStatus() == PaymentStatus.PAID) {
            amount = amount.add(new Money(payment.getPaymentRequest().getAmount()));
          }
        }
      }
      count++;
    }
    return InvoicesSummary.InvoiceSummaryContent.builder().count(count).amount(amount).build();
  }

  private InvoicesSummary.InvoiceSummaryContent filterUnpaidInvoicesSummary(
      List<Invoice> invoices) {
    int count = 0;
    Money amount = new Money();
    List<Invoice> filteredInvoices =
        invoices.stream()
            .filter(
                invoice ->
                    invoice.getArchiveStatus() == ArchiveStatus.ENABLED
                        && invoice.getStatus() == CONFIRMED)
            .toList();
    for (Invoice i : filteredInvoices) {
      if (i.getPaymentType() == CASH) {
        amount = amount.add(new Money(i.getTotalPriceWithVat()));
      } else {
        for (var payment : i.getPaymentRegulations()) {
          if (payment.getPaymentRequest().getStatus() == PaymentStatus.UNPAID) {
            amount = amount.add(new Money(payment.getPaymentRequest().getAmount()));
          }
        }
      }
      count++;
    }
    return InvoicesSummary.InvoiceSummaryContent.builder().count(count).amount(amount).build();
  }

  private InvoicesSummary.InvoiceSummaryContent filterProposalInvoicesSummary(
      List<Invoice> invoices) {
    int count = 0;
    Money amount = new Money();
    List<Invoice> filteredInvoices =
        invoices.stream()
            .filter(
                invoice ->
                    invoice.getStatus() == PROPOSAL
                        && invoice.getArchiveStatus() == ArchiveStatus.ENABLED)
            .toList();
    for (Invoice i : filteredInvoices) {
      if (i.getPaymentType() == CASH) {
        amount = amount.add(new Money(i.getTotalPriceWithVat()));
      } else {
        for (var payment : i.getPaymentRegulations()) {
          amount = amount.add(new Money(payment.getPaymentRequest().getAmount()));
        }
      }
      count++;
    }
    return InvoicesSummary.InvoiceSummaryContent.builder().count(count).amount(amount).build();
  }

  private static List<CreatePaymentRegulation> initPaymentReg(Invoice actual) {
    List<CreatePaymentRegulation> paymentReg = actual.getPaymentRegulations();
    paymentReg.forEach(
        payment -> {
          PaymentRequest request = payment.getPaymentRequest();
          request.setId(String.valueOf(randomUUID()));
          request.setExternalId(null);
          request.setPaymentUrl(null);
        });
    return paymentReg;
  }

  @Transactional
  public Invoice updatePaymentStatus(String invoiceId, String paymentId, PaymentMethod method) {
    boolean isUserUpdated = true;
    Invoice invoice = getById(invoiceId);
    PaymentRequest paymentRequest =
        invoice.getPaymentRegulations().stream()
            .filter(payment -> payment.getPaymentRequest().getId().equals(paymentId))
            .findAny()
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Invoice(id="
                            + invoiceId
                            + ") "
                            + "does not contain PaymentRequest(id="
                            + paymentId
                            + ")"))
            .getPaymentRequest();
    PaymentRequest toSave =
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
            .build();
    List<PaymentRequest> savedPayments = paymentRepository.saveAll(List.of(toSave));
    invoice
        .getPaymentRegulations()
        .forEach(
            payment -> {
              var request = payment.getPaymentRequest();
              if (request.getId().equals(paymentId)) {
                if (savedPayments.isEmpty()) {
                  throw new ApiException(
                      SERVER_EXCEPTION, "PaymentRequest(id=" + paymentId + ") was not saved");
                }
                payment.setPaymentRequest(savedPayments.get(0));
              }
            });
    boolean allPaymentsPaid =
        invoice.getPaymentRegulations().stream()
            .allMatch(payment -> payment.getPaymentRequest().getStatus() == PaymentStatus.PAID);
    if (allPaymentsPaid) {
      Invoice paidInvoice = invoice.toBuilder().status(PAID).paymentMethod(MULTIPLE).build();
      return crupdateInvoice(paidInvoice);
    }
    repositoryImpl.processAsPdf(invoice);
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
  public Invoice crupdateInvoice(Invoice invoice) {
    if (invoice.getCustomer() != null) {
      customerService.checkCustomerExistence(invoice.getCustomer());
    }
    if (!hasAvailableReference(invoice)) {
      throw new BadRequestException(
          "La référence " + invoice.getRealReference() + " est déjà utilisée");
    }
    if (!invoice.getActualHolder().isSubjectToVat()) {
      invoice.getProducts().forEach(product -> product.setVatPercent(new Fraction()));
    }
    Invoice actual = handleStatusChanges(invoice);
    return repository.crupdate(actual);
  }

  public List<Invoice> archiveInvoices(List<ArchiveInvoice> archiveInvoices) {
    return repository.saveAll(archiveInvoices);
  }

  public boolean hasAvailableReference(Invoice invoice) {
    String idUser = invoice.getUser().getId();
    String idInvoice = invoice.getId();
    String reference = invoice.getRealReference();
    InvoiceStatus status = invoice.getStatus();
    if (reference == null) {
      return true;
    }
    List<Invoice> existingInvoice = repository.findByIdUserAndRef(idUser, reference);

    /*Case when crupdating CONFIRMED invoice*/
    if (existingInvoice.size() == 1
        && existingInvoice.get(0).getStatus() == CONFIRMED
        && invoice.getStatus() == CONFIRMED) {
      return invoice.getId().equals(existingInvoice.get(0).getId());
    } else if ((invoice.getStatus() == CONFIRMED)
        && !existingInvoice.isEmpty()
        && existingInvoice.stream()
            .anyMatch(
                existing ->
                    existing.getStatus() == CONFIRMED
                        && existing.getId().equals(invoice.getId()))) {
      return true;
    }

    boolean isTobeConfirmed =
        existingInvoice.isEmpty()
            || existingInvoice.stream().anyMatch(existing -> existing.getStatus() == PROPOSAL);
    boolean isToBePaid =
        existingInvoice.stream().anyMatch(existing -> existing.getStatus() == CONFIRMED);
    return (status != CONFIRMED && status != PAID)
        ? (existingInvoice.isEmpty()
            || existingInvoice.stream().anyMatch(existing -> existing.getId().equals(idInvoice)))
        : (status == CONFIRMED ? isTobeConfirmed : isToBePaid);
  }

  private Invoice handleStatusChanges(Invoice invoice) {
    Invoice actual = invoice.toBuilder().build();
    actual.setPaymentRegulations(getPaymentRegWithoutUrl(actual));

    handleStatusesFromExistingInvoice(actual);

    if (actual.getStatus() == CONFIRMED || actual.getStatus() == PROPOSAL_CONFIRMED) {
      checkHolderMandatoryData(invoice);
      // TODO: check everything is ok before marking invoice as CONFIRMED
      // Example : check if account has IBAN and BIC ...
      if (actual.getPaymentType() == CASH) {
        handleCashType(actual);
      } else {
        handleMultipleRegType(invoice, actual);
      }
    }
    return actual;
  }

  private void checkHolderMandatoryData(Invoice invoice) {
    AccountHolder accountHolder = invoice.getActualHolder();
    StringBuilder builder = new StringBuilder();
    if (accountHolder.getAddress() == null) {
      builder.append("Account holder address is mandatory to confirm invoice");
    }
    if (accountHolder.getCountry() == null) {
      builder.append("Account holder country is mandatory to confirm invoice");
    }
    if (accountHolder.getCity() == null) {
      builder.append("Account holder city is mandatory to confirm invoice");
    }
    if (accountHolder.getPostalCode() == null) {
      builder.append("Account holder postal code is mandatory to confirm invoice");
    }
    String message = builder.toString();
    if (!message.isEmpty()) {
      throw new BadRequestException(message);
    }
  }

  private void handleStatusesFromExistingInvoice(Invoice actual) {
    Optional<Invoice> optionalInvoice = repository.pwFindOptionalById(actual.getId());
    if (optionalInvoice.isPresent()) {
      Invoice oldInvoice = optionalInvoice.get();
      actual.setFileId(oldInvoice.getFileId());

      if (actual.getStatus() == CONFIRMED && oldInvoice.getStatus() == PROPOSAL) {
        // To be saved later as another HInvoice with diff status but same reference
        actual.setStatus(PROPOSAL_CONFIRMED);
      } else if (actual.getStatus() == PAID && oldInvoice.getStatus() == CONFIRMED) {
        actual.setValidityDate(null);
        actual.setSendingDate(oldInvoice.getSendingDate());
        if (actual.getPaymentType() == CASH) {
          actual.setPaymentUrl(oldInvoice.getPaymentUrl());
          actual.setToPayAt(actual.getSendingDate().plusDays(actual.getDelayInPaymentAllowed()));
        } else {
          actual.setPaymentRegulations(oldInvoice.getPaymentRegulations());
        }
      }
    }
  }

  private void handleCashType(Invoice actual) {
    Integer delayInPaymentAllowed = actual.getDelayInPaymentAllowed();
    if (delayInPaymentAllowed == null) {
      log.warn(
          "Delay in payment allowed is mandatory to retrieve invoice payment date limit."
              + " 30 days are given by default");
      delayInPaymentAllowed = DEFAULT_TO_PAY_DELAY_DAYS;
    }
    actual.setToPayAt(actual.getSendingDate().plusDays(delayInPaymentAllowed));
    actual.setPaymentUrl(
        actual.getTotalPriceWithVat().getCentsAsDecimal() != 0
            ? pis.initiateInvoicePayment(actual).getRedirectUrl()
            : actual.getPaymentUrl());
  }

  private void handleMultipleRegType(Invoice invoice, Invoice actual) {
    // TODO: check amount changes before creating new payments to optimize perf
    List<CreatePaymentRegulation> paymentRegWithUrl = getPaymentRegWithUrl(invoice);
    actual.setPaymentRegulations(paymentRegWithUrl);
    actual.setPaymentUrl(null);
  }

  private List<CreatePaymentRegulation> getPaymentRegWithUrl(Invoice actual) {
    List<PaymentInitiation> paymentInitiations = getPaymentInitiations(actual);
    List<PaymentRequest> paymentRequests =
        pis.retrievePaymentEntitiesWithUrl(paymentInitiations, actual.getId(), actual.getUser());
    return convertPaymentRequests(paymentRequests);
  }

  private List<CreatePaymentRegulation> getPaymentRegWithoutUrl(Invoice actual) {
    List<PaymentInitiation> paymentInitiations = getPaymentInitiations(actual);
    List<PaymentRequest> paymentRequests =
        pis.retrievePaymentEntities(paymentInitiations, actual.getId(), actual.getStatus());
    return convertPaymentRequests(paymentRequests);
  }

  private List<CreatePaymentRegulation> convertPaymentRequests(
      List<PaymentRequest> paymentRequests) {
    Fraction totalPrice = computeTotalPriceFromPaymentReq(paymentRequests);
    return paymentRequests.stream()
        .map(
            payment -> {
              Fraction percent =
                  totalPrice.getCentsRoundUp() == 0
                      ? new Fraction()
                      : payment.getAmount().operate(totalPrice, Aprational::divide);
              return requestMapper.toPaymentRegulation(payment, percent);
            })
        .collect(Collectors.toList());
  }

  private List<PaymentInitiation> getPaymentInitiations(Invoice domain) {
    List<CreatePaymentRegulation> paymentReg = domain.getPaymentRegulations();
    List<PaymentInitiation> payments =
        paymentReg.stream()
            .map(
                payment -> {
                  String randomId = String.valueOf(randomUUID());
                  PaymentRequest paymentRequest = payment.getPaymentRequest();
                  paymentRequest.setExternalId(randomId);
                  String label = paymentRequest.getLabel();
                  String reference =
                      paymentRequest.getReference() == null
                          ? domain.getRealReference()
                          : paymentRequest.getReference();
                  return requestMapper.convertFromInvoice(
                      randomId,
                      label,
                      reference,
                      domain,
                      payment,
                      paymentRequest.getPaymentHistoryStatus());
                })
            .sorted(Comparator.comparing(PaymentInitiation::getPaymentDueDate))
            .toList();
    for (int i = 0; i < payments.size(); i++) {
      PaymentInitiation paymentInitiation = payments.get(i);
      if (paymentInitiation.getLabel() == null) {
        if (i != payments.size() - 1) {
          paymentInitiation.setLabel(domain.getTitle() + " - Acompte N°" + (i + 1));
        } else {
          paymentInitiation.setLabel(domain.getTitle() + " - Restant dû");
        }
      }
    }
    return payments;
  }

  @Transactional
  public Invoice duplicateAsDraft(String idInvoice, String reference) {
    Invoice actual = getById(idInvoice);
    List<CreatePaymentRegulation> paymentRegulations = initPaymentReg(actual);
    Invoice duplicatedInvoice =
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
}
