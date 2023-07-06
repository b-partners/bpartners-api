package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.ArchiveInvoice;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import app.bpartners.api.repository.InvoiceRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apfloat.Aprational;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum.CASH;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL_CONFIRMED;
import static app.bpartners.api.service.utils.PaymentUtils.computeTotalPriceFromPaymentReq;
import static java.util.UUID.randomUUID;

@Service
@AllArgsConstructor
@Slf4j
public class InvoiceService {
  public static final String INVOICE_TEMPLATE = "invoice";
  public static final String DRAFT_TEMPLATE = "draft";
  public static final String DRAFT_REF_PREFIX = "BROUILLON-";
  public static final String PROPOSAL_REF_PREFIX = "DEVIS-";
  private final InvoiceRepository repository;
  private final CustomerService customerService;
  private final PaymentInitiationService pis;
  private final PaymentRequestMapper requestMapper;

  public List<Invoice> getInvoices(
      String idUser, PageFromOne page, BoundedPageSize pageSize, InvoiceStatus status) {
    int pageValue = page != null ? page.getValue() - 1 : 0;
    int pageSizeValue = pageSize != null ? pageSize.getValue() : 30;
    if (status != null) {
      return repository.findAllByIdUserAndStatus(idUser, status, pageValue, pageSizeValue);
    }
    return repository.findAllByIdUser(idUser, pageValue, pageSizeValue);
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
      invoice.getProducts().forEach(
          product -> product.setVatPercent(new Fraction())
      );
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
    List<Invoice> existingInvoice =
        repository.findByIdUserAndRef(idUser, reference);

    boolean isTobeConfirmed = existingInvoice.stream()
        .anyMatch(existing -> existing.getStatus() == PROPOSAL);
    boolean isToBePaid = existingInvoice.stream()
        .anyMatch(existing -> existing.getStatus() == CONFIRMED);
    return (status != CONFIRMED && status != PAID)
        ? existingInvoice.isEmpty() || existingInvoice.stream()
        .anyMatch(existing -> existing.getId().equals(idInvoice))
        : status == CONFIRMED ? isTobeConfirmed
        : isToBePaid;
  }

  private Invoice handleStatusChanges(Invoice invoice) {
    Invoice actual = invoice.toBuilder().build();
    actual.setPaymentRegulations(getPaymentRegWithoutUrl(actual));

    handleStatusesFromExistingInvoice(actual);

    if (actual.getStatus() == CONFIRMED || actual.getStatus() == PROPOSAL_CONFIRMED) {
      checkHolderMandatoryData(invoice);
      //TODO: check everything is ok before marking invoice as CONFIRMED
      //Example : check if account has IBAN and BIC ...
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
        //To be saved later as another HInvoice with diff status but same reference
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
      delayInPaymentAllowed = 30;
    }
    actual.setToPayAt(actual.getSendingDate().plusDays(delayInPaymentAllowed));
    actual.setPaymentUrl(actual.getTotalPriceWithVat().getCentsAsDecimal() != 0
        ? pis.initiateInvoicePayment(actual).getRedirectUrl()
        : actual.getPaymentUrl());
  }

  private void handleMultipleRegType(Invoice invoice, Invoice actual) {
    //TODO: check amount changes before creating new payments to optimize perf
    List<CreatePaymentRegulation> paymentRegWithUrl = getPaymentRegWithUrl(invoice);
    actual.setPaymentRegulations(paymentRegWithUrl);
    actual.setPaymentUrl(null);
  }


  private List<CreatePaymentRegulation> getPaymentRegWithUrl(Invoice actual) {
    List<PaymentInitiation> paymentInitiations = getPaymentInitiations(actual);
    List<PaymentRequest> paymentRequests =
        pis.retrievePaymentEntitiesWithUrl(paymentInitiations, actual.getId());
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
        .map(payment -> {
          Fraction percent = totalPrice.getCentsRoundUp() == 0 ? new Fraction()
              : payment.getAmount().operate(totalPrice,
              Aprational::divide);
          return requestMapper.toPaymentRegulation(payment, percent);
        })
        .collect(Collectors.toList());
  }

  private List<PaymentInitiation> getPaymentInitiations(Invoice domain) {
    List<PaymentInitiation> payments = domain.getPaymentRegulations().stream()
        .map(payment -> {
          String randomId = String.valueOf(randomUUID());
          payment.getPaymentRequest().setExternalId(randomId);
          return requestMapper.convertFromInvoice(
              randomId, domain, payment);
        })
        .sorted(Comparator.comparing(PaymentInitiation::getPaymentDueDate))
        .collect(Collectors.toUnmodifiableList());
    for (int i = 0; i < payments.size(); i++) {
      if (i != payments.size() - 1) {
        payments.get(i).setLabel(domain.getTitle() + " - Acompte N°" + (i + 1));
      } else {
        payments.get(i).setLabel(domain.getTitle() + " - Restant dû");
      }
    }
    return payments;
  }
}