package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.ProductStatus.ENABLED;
import static app.bpartners.api.model.mapper.InvoiceMapper.computePriceNoVatWithDiscount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computePriceWithoutDiscount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computeTotalPriceWithVatAndDiscount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computeTotalVatWithDiscount;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.SubscriptionPaymentInvoiceCreated;
import app.bpartners.api.endpoint.event.model.SubscriptionPaymentInvoiceRequested;
import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum;
import app.bpartners.api.endpoint.rest.model.PaymentMethod;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceDiscount;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.service.customer.SubscriptionCustomerResolver;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.invoice.ReferenceGenerator;
import app.bpartners.api.service.subscription.SubscriptionPaymentService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionPaymentInvoiceRequestedService
    implements Consumer<SubscriptionPaymentInvoiceRequested> {
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private final SubscriptionPaymentRepository subscriptionPaymentRepository;
  private final SubscriptionPaymentService subscriptionPaymentService;
  private final UserRepository userRepository;
  private final UserSubscriptionConf userSubscriptionConf;
  private final SubscriptionCustomerResolver subscriptionCustomerResolver;
  private final InvoiceService invoiceService;
  private final CustomDateFormatter customDateFormatter;
  private final EventProducer eventProducer;

  @Override
  public void accept(SubscriptionPaymentInvoiceRequested event) {
    var subscriptionPaymentIdentifier = event.getSubscriptionPaymentId();
    var optionalSubscriptionPayment =
        subscriptionPaymentRepository.findById(subscriptionPaymentIdentifier);
    if (optionalSubscriptionPayment.isEmpty()) {
      log.warn("No SubscriptionPayment.id={} to invoice, skipping", subscriptionPaymentIdentifier);
      return;
    }
    var subscriptionPayment = optionalSubscriptionPayment.get();
    if (subscriptionPayment.getInvoiceId() != null) {
      log.info(
          "SubscriptionPayment(id={}) is already invoiced by Invoice(id={}), skipping",
          subscriptionPayment.getId(),
          subscriptionPayment.getInvoiceId());
      return;
    }

    var userToCredit = userRepository.getById(userSubscriptionConf.getUserToCreditId());
    var userToDebit = userRepository.getById(subscriptionPayment.getUserId());
    var customerToDebit = subscriptionCustomerResolver.apply(userToCredit, userToDebit);

    var createdInvoice =
        invoiceService.crupdateSubscriptionInvoice(
            computeSubscriptionInvoice(userToCredit, customerToDebit, subscriptionPayment));
    subscriptionPaymentService.invoicedBy(subscriptionPayment, createdInvoice.getId());
    log.info(
        "Invoice(id={}, ref={}) created for SubscriptionPayment(id={}) of User(id={})",
        createdInvoice.getId(),
        createdInvoice.getRef(),
        subscriptionPayment.getId(),
        userToDebit.getId());
    eventProducer.accept(
        List.of(
            SubscriptionPaymentInvoiceCreated.builder()
                .invoiceId(createdInvoice.getId())
                .subscriptionPaymentId(subscriptionPayment.getId())
                .build()));
  }

  private Invoice computeSubscriptionInvoice(
      User userToCredit, Customer customerToDebit, SubscriptionPayment subscriptionPayment) {
    var invoiceIdentifier = randomUUID().toString();
    var paidAt = paidAt(subscriptionPayment);
    var sendingDate = paidAt.atZone(PARIS).toLocalDate();
    var invoiceProducts = computeSubscriptionProducts(invoiceIdentifier, subscriptionPayment);
    var discountZero = new Fraction(BigInteger.ZERO);
    var referenceGenerator = new ReferenceGenerator(() -> LocalDateTime.ofInstant(paidAt, PARIS));
    return Invoice.builder()
        .id(invoiceIdentifier)
        .ref(referenceGenerator.get())
        .title(titleOf(subscriptionPayment, paidAt))
        .subscriptionInvoice(true)
        .status(PAID)
        .archiveStatus(ArchiveStatus.ENABLED)
        .customer(customerToDebit)
        .toPayAt(sendingDate)
        .sendingDate(sendingDate)
        .validityDate(null)
        .paymentMethod(PaymentMethod.CREDIT_CARD)
        .user(userToCredit)
        .paymentType(PaymentTypeEnum.CASH)
        .paymentRegulations(new ArrayList<>())
        .products(invoiceProducts)
        .totalPriceWithoutDiscount(computePriceWithoutDiscount(invoiceProducts))
        .totalPriceWithoutVat(computePriceNoVatWithDiscount(discountZero, invoiceProducts))
        .totalVat(computeTotalVatWithDiscount(discountZero, invoiceProducts))
        .totalPriceWithVat(computeTotalPriceWithVatAndDiscount(discountZero, invoiceProducts))
        .delayInPaymentAllowed(0)
        .discount(InvoiceDiscount.builder().percentValue(new Fraction(BigInteger.ZERO)).build())
        .createdAt(now())
        .delayPenaltyPercent(new Fraction(BigInteger.ZERO))
        .build();
  }

  private String titleOf(SubscriptionPayment subscriptionPayment, Instant paidAt) {
    var billedPeriod = billedPeriodOf(subscriptionPayment);
    return billedPeriod == null
        ? "Facture d'abonnement du " + customDateFormatter.formatFrenchDate(paidAt)
        : "Facture d'abonnement " + billedPeriod;
  }

  private String billedPeriodOf(SubscriptionPayment subscriptionPayment) {
    var periodStart = subscriptionPayment.getPeriodStartDatetime();
    var periodEnd = subscriptionPayment.getPeriodEndDatetime();
    if (periodStart == null || periodEnd == null) {
      return null;
    }
    return "pour la période du "
        + customDateFormatter.formatFrenchDate(periodStart)
        + " au "
        + customDateFormatter.formatFrenchDate(periodEnd);
  }

  private Instant paidAt(SubscriptionPayment subscriptionPayment) {
    return subscriptionPayment.getPaymentDatetime() == null
        ? now()
        : subscriptionPayment.getPaymentDatetime();
  }

  private List<InvoiceProduct> computeSubscriptionProducts(
      String invoiceIdentifier, SubscriptionPayment subscriptionPayment) {
    var invoiceProducts = new ArrayList<InvoiceProduct>();
    invoiceProducts.add(
        InvoiceProduct.builder()
            .id(randomUUID().toString())
            .idInvoice(invoiceIdentifier)
            .createdAt(now())
            .description(subscriptionPayment.paymentLabel())
            .quantity(1)
            .unitPrice(
                new Fraction(
                    BigInteger.valueOf(subscriptionPayment.amountInCentsWithoutVatOrZero())))
            .vatPercent(new Fraction(BigInteger.valueOf(subscriptionPayment.vatPercentOrZero())))
            .status(ENABLED)
            .build());
    return invoiceProducts;
  }
}
