package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.model.credit.CreditPurchaseStatus.COMPLETED;
import static app.bpartners.api.model.mapper.InvoiceMapper.computePriceNoVatWithDiscount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computePriceWithoutDiscount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computeTotalPriceWithVatAndDiscount;
import static app.bpartners.api.model.mapper.InvoiceMapper.computeTotalVatWithDiscount;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.CreditOperationInvoiceCreated;
import app.bpartners.api.endpoint.event.model.CreditOperationInvoiceRequested;
import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum;
import app.bpartners.api.endpoint.rest.model.PaymentMethod;
import app.bpartners.api.endpoint.rest.model.ProductStatus;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceDiscount;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.User;
import app.bpartners.api.model.credit.CreditPurchase;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.CreditPurchaseRepository;
import app.bpartners.api.service.customer.SubscriptionCustomerResolver;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.invoice.ReferenceGenerator;
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
public class CreditOperationInvoiceRequestedService
    implements Consumer<CreditOperationInvoiceRequested> {
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private static final int VALIDITY_DELAY_DAYS = 30;
  private final CreditPurchaseRepository creditPurchaseRepository;
  private final UserRepository userRepository;
  private final UserSubscriptionConf userSubscriptionConf;
  private final SubscriptionCustomerResolver subscriptionCustomerResolver;
  private final InvoiceService invoiceService;
  private final CustomDateFormatter customDateFormatter;
  private final EventProducer eventProducer;

  @Override
  public void accept(CreditOperationInvoiceRequested event) {
    var creditTransaction = event.getCreditTransaction();
    if (creditTransaction == null || !creditTransaction.isPurchaseCredit()) {
      log.info("CreditTransaction {} is not a credit purchase, skipping", creditTransaction);
      return;
    }
    var creditPurchaseIdentifier = creditTransaction.getCreditPurchaseId();
    if (creditPurchaseIdentifier == null) {
      log.warn(
          "CreditTransaction(id={}) is a purchase without creditPurchaseId, no invoice to generate",
          creditTransaction.getId());
      return;
    }
    var optionalCreditPurchase = creditPurchaseRepository.findById(creditPurchaseIdentifier);
    if (optionalCreditPurchase.isEmpty()) {
      log.warn("No CreditPurchase.id={} to invoice, skipping", creditPurchaseIdentifier);
      return;
    }
    var creditPurchase = optionalCreditPurchase.get();
    if (creditPurchase.getInvoiceId() != null) {
      log.info(
          "CreditPurchase(id={}) is already invoiced by Invoice(id={}), skipping",
          creditPurchase.getId(),
          creditPurchase.getInvoiceId());
      return;
    }

    var userToCredit = userRepository.getById(userSubscriptionConf.getUserToCreditId());
    var userToDebit = userRepository.getById(creditTransaction.getUserId());
    var customerToDebit = subscriptionCustomerResolver.apply(userToCredit, userToDebit);

    var createdInvoice =
        invoiceService.crupdateSubscriptionInvoice(
            computeCreditPurchaseInvoice(
                userToCredit, customerToDebit, creditPurchase, creditTransaction));
    creditPurchaseRepository.save(
        creditPurchase.toBuilder().invoiceId(createdInvoice.getId()).build());
    log.info(
        "Invoice(id={}, ref={}) created for CreditPurchase(id={}) of User(id={})",
        createdInvoice.getId(),
        createdInvoice.getRef(),
        creditPurchase.getId(),
        userToDebit.getId());
    eventProducer.accept(
        List.of(
            CreditOperationInvoiceCreated.builder()
                .invoiceId(createdInvoice.getId())
                .creditPurchaseId(creditPurchase.getId())
                .build()));
  }

  private Invoice computeCreditPurchaseInvoice(
      User userToCredit,
      Customer customerToDebit,
      CreditPurchase creditPurchase,
      CreditTransaction creditTransaction) {
    var invoiceIdentifier = randomUUID().toString();
    var purchasedAt = purchasedAt(creditPurchase, creditTransaction);
    var sendingDate = purchasedAt.atZone(PARIS).toLocalDate();
    var invoiceProducts =
        computeCreditProducts(invoiceIdentifier, creditPurchase, creditTransaction);
    var discountZero = new Fraction(BigInteger.ZERO);
    var referenceGenerator =
        new ReferenceGenerator(() -> LocalDateTime.ofInstant(purchasedAt, PARIS));
    var alreadyPaid = COMPLETED.equals(creditPurchase.getStatus());
    return Invoice.builder()
        .id(invoiceIdentifier)
        .ref(referenceGenerator.get())
        .title(titleOf(purchasedAt))
        .subscriptionInvoice(true)
        .status(alreadyPaid ? PAID : CONFIRMED)
        .archiveStatus(ArchiveStatus.ENABLED)
        .customer(customerToDebit)
        .toPayAt(sendingDate)
        .sendingDate(sendingDate)
        .validityDate(alreadyPaid ? null : sendingDate.plusDays(VALIDITY_DELAY_DAYS))
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

  private String titleOf(Instant purchasedAt) {
    return "Facture achat de crédits du " + customDateFormatter.formatFrenchDate(purchasedAt);
  }

  private Instant purchasedAt(CreditPurchase creditPurchase, CreditTransaction creditTransaction) {
    if (creditPurchase.getCompletionDatetime() != null) {
      return creditPurchase.getCompletionDatetime();
    }
    return creditTransaction.getCreationDatetime() == null
        ? now()
        : creditTransaction.getCreationDatetime();
  }

  private List<InvoiceProduct> computeCreditProducts(
      String invoiceIdentifier,
      CreditPurchase creditPurchase,
      CreditTransaction creditTransaction) {
    var unitPrice = creditPurchase.unitPriceApplied();
    var purchasedCredits = creditTransaction.creditsOrZero();
    var lineQuantity = lineQuantity(creditPurchase, purchasedCredits);
    var lineUnitPriceInCents =
        lineQuantity == 0
            ? unitPrice.inCentsWithoutVat()
            : unitPrice.inCentsWithoutVat() * (purchasedCredits / lineQuantity);
    var invoiceProducts = new ArrayList<InvoiceProduct>();
    invoiceProducts.add(
        InvoiceProduct.builder()
            .id(randomUUID().toString())
            .idInvoice(invoiceIdentifier)
            .createdAt(now())
            .description(creditPurchase.invoiceLineLabel())
            .quantity(lineQuantity)
            .unitPrice(new Fraction(BigInteger.valueOf(lineUnitPriceInCents)))
            .vatPercent(new Fraction(BigInteger.valueOf(unitPrice.vatPercent())))
            .status(ProductStatus.ENABLED)
            .build());
    return invoiceProducts;
  }

  private int lineQuantity(CreditPurchase creditPurchase, long purchasedCredits) {
    if (creditPurchase.isCustomPurchase()) {
      return (int) purchasedCredits;
    }
    var packQuantity = creditPurchase.packQuantity();
    return purchasedCredits % packQuantity == 0 ? packQuantity : 1;
  }
}
