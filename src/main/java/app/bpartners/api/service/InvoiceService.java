package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.TypedInvoiceCrupdated;
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
import app.bpartners.api.model.validator.InvoiceValidator;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.ProductRepository;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.apfloat.Aprational;
import org.springframework.stereotype.Service;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static app.bpartners.api.service.utils.FractionUtils.toAprational;

@Service
@AllArgsConstructor
public class InvoiceService {
  public static final String DRAFT_REF_SUFFIX = "-TMP";
  private final InvoiceRepository repository;
  private final ProductRepository productRepository;
  private final PaymentInitiationService pis;
  private final AccountHolderService holderService;
  private final PrincipalProvider auth;
  private final InvoiceValidator validator;
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

  private TypedInvoiceCrupdated toTypedEvent(Invoice invoice, AccountHolder accountHolder) {
    return new TypedInvoiceCrupdated(InvoiceCrupdated.builder()
        .invoice(invoice)
        .accountHolder(accountHolder)
        .logoFileId(userLogoFileId())
        .build());
  }

  private String userLogoFileId() {
    return ((Principal) auth.getAuthentication().getPrincipal()).getUser().getLogoFileId();
  }
}