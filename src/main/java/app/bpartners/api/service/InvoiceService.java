package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.TypedInvoiceCrupdated;
import app.bpartners.api.endpoint.event.model.TypedMailSent;
import app.bpartners.api.endpoint.event.model.gen.InvoiceCrupdated;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.model.Product;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.validator.InvoiceValidator;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.ProductRepository;
import app.bpartners.api.service.aws.SesService;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.apfloat.Aprational;
import org.springframework.stereotype.Service;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static app.bpartners.api.service.utils.FileInfoUtils.PDF_EXTENSION;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static app.bpartners.api.service.utils.FractionUtils.toAprational;

@Service
@AllArgsConstructor
public class InvoiceService {
  public static final String DRAFT_REF_SUFFIX = "-TMP";
  private final InvoiceRepository repository;
  private final ProductRepository productRepository;
  private final PaymentInitiationService pis;
  private final FileService fileService;
  private final AccountHolderService holderService;
  private final PrincipalProvider auth;
  private final InvoiceValidator validator;
  private final SesService sesService;
  private final EventProducer eventProducer;

  public List<Invoice> getInvoices(String accountId, PageFromOne page, BoundedPageSize pageSize,
                                   InvoiceStatus status) {
    int pageValue = page.getValue() - 1;
    int pageSizeValue = pageSize.getValue();
    List<Invoice> invoices = repository.findAllByAccountId(accountId, pageValue, pageSizeValue);
    if (status != null) {
      invoices = repository.findAllByAccountIdAndStatus(accountId, status,
          pageValue,
          pageSizeValue);
    }
    return invoices.stream()
        .map(this::refreshValues)
        .collect(Collectors.toUnmodifiableList());
  }

  public Invoice getById(String invoiceId) {
    return refreshValues(repository.getById(invoiceId));
  }

  @Transactional
  public Invoice crupdateInvoice(Invoice toCrupdate) {
    validator.accept(toCrupdate);

    Invoice refreshedInvoice = refreshValues(repository.crupdate(toCrupdate));
    AccountHolder accountHolder =
        holderService.getAccountHolderByAccountId(refreshedInvoice.getAccount().getId());
    eventProducer.accept(List.of(toTypedEvent(refreshedInvoice, accountHolder)));

    return refreshedInvoice;
  }

  public void sendInvoice(
      String invoiceId, String subject, String emailMessage) {
    Invoice invoice = getById(invoiceId);
    byte[] fileAsBytes = fileService.downloadFile(INVOICE, invoice.getAccount().getId(),
        invoice.getFileId());
    if (!invoice.getStatus().equals(DRAFT)) {
      eventProducer.accept(List.of(getMailSentEvent(invoice, subject, emailMessage, fileAsBytes)));
    } else {
      throw new BadRequestException(
          "Invoice." + invoiceId + " can not be sent because status is " + invoice.getStatus());
    }
  }


  private Invoice refreshValues(Invoice invoice) {
    List<Product> products = invoice.getProducts();
    if (products.isEmpty()) {
      products =
          productRepository.findByIdInvoice(invoice.getId());
    }
    Invoice initializedInvoice = Invoice.builder()
        .id(invoice.getId())
        .fileId(invoice.getFileId())
        .comment(invoice.getComment())
        .updatedAt(invoice.getUpdatedAt())
        .title(invoice.getTitle())
        .invoiceCustomer(invoice.getInvoiceCustomer())
        .account(invoice.getAccount())
        .status(invoice.getStatus())
        .totalVat(computeTotalVat(products))
        .totalPriceWithoutVat(computeTotalPriceWithoutVat(products))
        .totalPriceWithVat(computeTotalPriceWithVat(products))
        .products(products)
        .toPayAt(invoice.getToPayAt())
        .sendingDate(invoice.getSendingDate())
        .build();
    if (invoice.getStatus().equals(CONFIRMED) || invoice.getStatus().equals(PAID)) {
      PaymentRedirection paymentRedirection = pis.initiateInvoicePayment(initializedInvoice);
      initializedInvoice.setPaymentUrl(paymentRedirection.getRedirectUrl());
      initializedInvoice.setRef(invoice.getRef());
    } else {
      initializedInvoice.setPaymentUrl(null);
      initializedInvoice.setRef(invoice.getRef() + DRAFT_REF_SUFFIX);
    }
    return initializedInvoice;
  }

  private Fraction computeTotalVat(List<Product> products) {
    return computeSum(products, products.stream()
        .map(Product::getTotalVat));
  }

  private Fraction computeTotalPriceWithoutVat(List<Product> products) {
    return computeSum(products, products.stream()
        .map(Product::getTotalWithoutVat));
  }

  private Fraction computeTotalPriceWithVat(List<Product> products) {
    return computeSum(products, products.stream()
        .map(Product::getTotalPriceWithVat));
  }

  private Fraction computeSum(List<Product> products, Stream<Fraction> fractionStream) {
    if (products == null) {
      return new Fraction();
    }
    Aprational aprational = fractionStream
        .map(a -> toAprational(a.getNumerator(), a.getDenominator()))
        .reduce(new Aprational(0), Aprational::add);
    return parseFraction(aprational);
  }

  private TypedMailSent getMailSentEvent(
      Invoice invoice, String subject, String emailMessage, byte[] pdf) {
    if (!invoice.getStatus().equals(DRAFT)) {
      return toTypedEvent(invoice, subject, emailMessage, pdf);
    }
    throw new BadRequestException("Invoice." + invoice.getId() + " can not be sent because "
        + "status is " + invoice.getStatus());
  }

  //TODO: set it again in template resolver when the load style baseUrl is set
  private String emailBody(String emailMessage, Invoice invoice) {
    AccountHolder accountHolder =
        holderService.getAccountHolderByAccountId(invoice.getAccount().getId());
    return "<html>\n"
        + "    <body style=\"font-family: 'Gill Sans'\">\n"
        + "        <h2 style=color:#8d2158;>" + accountHolder.getName() + "</h2>\n"
        + emailMessage
        + "        <p>Bien à vous et merci pour votre confiance.</p>\n"
        + "    </body>\n"
        + "</html>";
  }

  //TODO: persist the default email message and get it instead
  private String defaultEmailMessage(String type, Invoice invoice) {
    return "        <p>Bonjour,</p>\n"
        + "        <p>\n"
        + "            Retrouvez-ci joint votre " + type + " enregistré à la référence "
        + invoice.getRef() + "\n"
        + "        </p>\n";
  }

  private TypedMailSent toTypedEvent(Invoice invoice,
                                     String subject, String emailMessage, byte[] pdf) {
    String type = getStatusValue(invoice.getStatus());
    if (subject == null) {
      //TODO: check if the invoice has already been relaunched then change this
      subject = type + " " + invoice.getRef();
    }
    if (emailMessage == null) {
      emailMessage = defaultEmailMessage(type.toLowerCase(), invoice);
    }
    String recipient = invoice.getInvoiceCustomer().getEmail();
    return sesService.toTypedEvent(
        recipient, subject, emailBody(emailMessage, invoice), subject + PDF_EXTENSION, pdf);
  }

  private TypedInvoiceCrupdated toTypedEvent(Invoice invoice, AccountHolder accountHolder) {
    return new TypedInvoiceCrupdated(InvoiceCrupdated.builder()
        .invoice(invoice)
        .accountHolder(accountHolder)
        .logoFileId(userLogoFileId())
        .build());
  }

  private String getStatusValue(InvoiceStatus status) {
    if (status.equals(PROPOSAL) || status.equals(DRAFT)) {
      return "Devis";
    }
    if (status.equals(CONFIRMED) || status.equals(PAID)) {
      return "Facture";
    }
    throw new BadRequestException("Unknown status : " + status);
  }

  private String userLogoFileId() {
    return ((Principal) auth.getAuthentication().getPrincipal()).getUser().getLogoFileId();
  }
}