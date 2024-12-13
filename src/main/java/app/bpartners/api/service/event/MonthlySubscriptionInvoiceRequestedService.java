package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static app.bpartners.api.model.PageFromOne.MIN_PAGE;
import static app.bpartners.api.model.mapper.InvoiceMapper.*;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceCreated;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceRequested;
import app.bpartners.api.endpoint.rest.model.*;
import app.bpartners.api.model.*;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceDiscount;
import app.bpartners.api.model.User;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.InvoiceService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.MonthUtils;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MonthlySubscriptionInvoiceRequestedService
    implements Consumer<MonthlySubscriptionInvoiceRequested> {
  private final InvoiceService invoiceService;
  private final UserRepository userRepository;
  private final CustomerRepository customerRepository;
  private final SubscriptionService subscriptionService;
  private final EventProducer eventProducer;
  private final UserSubscriptionConf userSubscriptionConf;
  private final CustomDateFormatter customDateFormatter;
  private final MonthUtils monthUtils;

  @Override
  public void accept(MonthlySubscriptionInvoiceRequested event) {
    var criteria = new HashMap<String, Object>();
    criteria.put("status", ENABLED);
    criteria.put("page", event.getUserPage());
    criteria.put("pageSize", MAX_SIZE);
    // TODO: add hasSubscriptionStatus criteria

    var userToCredit = userRepository.getById(userSubscriptionConf.getUserToCreditId());
    var users = userRepository.findAllByCriteria(criteria);
    users.forEach(
        userToDebit -> {
          var monthlySubscriptionInvoice =
              computeMonthlySusbcriptionInvoice(userToCredit, userToDebit);
          var createdInvoice = invoiceService.crupdateInvoice(monthlySubscriptionInvoice);

          eventProducer.accept(
              List.of(
                  MonthlySubscriptionInvoiceCreated.builder()
                      .invoiceId(createdInvoice.getId())
                      .build()));
        });
  }

  private Invoice computeMonthlySusbcriptionInvoice(User userToCredit, User userToDebit) {
    var customerToDebit = computeCustomerToDebit(userToCredit, userToDebit);
    var invoiceId = randomUUID().toString();
    var invoiceTitle =
        "Abonnement Essentiel pour la période de "
            + customDateFormatter.formatFrenchDate(monthUtils.startOfActualMonth())
            + " au "
            + customDateFormatter.formatFrenchDate(monthUtils.endOfActualMonth());
    var invoiceProducts = computeSubscriptionProducts(invoiceId, invoiceTitle, userToDebit);
    var discountZero = new Fraction(BigInteger.ZERO);
    return Invoice.builder()
        .id(invoiceId)
        .ref("TODO: custom reference ? " + randomUUID())
        .title(invoiceTitle)
        .subscriptionInvoice(true)
        .status(CONFIRMED)
        .archiveStatus(ArchiveStatus.ENABLED)
        .customer(customerToDebit)
        .toPayAt(monthUtils.fifthOfNextMonth())
        .sendingDate(now())
        .validityDate(now().plusDays(30L))
        .paymentMethod(PaymentMethod.CREDIT_CARD)
        .user(userToCredit)
        .paymentType(app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum.CASH)
        .paymentRegulations(new ArrayList<>())
        .products(invoiceProducts)
        .totalPriceWithoutDiscount(computePriceWithoutDiscount(invoiceProducts))
        .totalPriceWithoutVat(computePriceNoVatWithDiscount(discountZero, invoiceProducts))
        .totalVat(computeTotalVatWithDiscount(discountZero, invoiceProducts))
        .totalPriceWithVat(computeTotalPriceWithVatAndDiscount(discountZero, invoiceProducts))
        .delayInPaymentAllowed(0)
        .discount(InvoiceDiscount.builder().percentValue(new Fraction(BigInteger.ZERO)).build())
        .createdAt(Instant.now())
        .delayPenaltyPercent(new Fraction(BigInteger.ZERO))
        .build();
  }

  private Customer computeCustomerToDebit(User userToCredit, User userToDebit) {
    var optionalCustomerToDebit =
        customerRepository
            .findByIdUserAndCriteria(
                userToCredit.getId(),
                null,
                null,
                userToDebit.getEmail(),
                null,
                null,
                null,
                null,
                null,
                CustomerStatus.ENABLED,
                MIN_PAGE,
                MAX_SIZE)
            .stream()
            .findAny();
    return optionalCustomerToDebit.orElseGet(() -> computeCustomerFromUserToDebit(userToDebit));
  }

  private Customer computeCustomerFromUserToDebit(User userToDebit) {
    var accountHolderToDebit = userToDebit.getDefaultHolder();
    return customerRepository.save(
        Customer.builder()
            .id(randomUUID().toString())
            .idUser(userToDebit.getId())
            .name(accountHolderToDebit.getName())
            .firstName(null) // because customers are company
            .lastName(null) // because customers are company
            .email(userToDebit.getEmail())
            .phone(accountHolderToDebit.getMobilePhoneNumber())
            .website(accountHolderToDebit.getWebsite())
            .address(accountHolderToDebit.getAddress())
            .zipCode(
                accountHolderToDebit.getPostalCode() == null
                    ? null
                    : Integer.valueOf(accountHolderToDebit.getPostalCode()))
            .city(accountHolderToDebit.getCity())
            .country(accountHolderToDebit.getCountry())
            .comment(null)
            .location(null) // TODO: compute location ?
            .status(CustomerStatus.ENABLED)
            .customerType(CustomerType.PROFESSIONAL)
            .recentlyAdded(false)
            .updatedAt(Instant.now())
            .createdAt(Instant.now())
            .build());
  }

  private @NotNull ArrayList<InvoiceProduct> computeSubscriptionProducts(
      String invoiceId, String invoiceTitle, User userToDebit) {
    var invoiceProducts = new ArrayList<InvoiceProduct>();
    var userSubscription = subscriptionService.getSubscriptionByUser(userToDebit);
    var latestSubscription = userSubscription.getLatestSubscription();

    var subscriptionProduct = latestSubscription.getSubscriptionProduct();
    invoiceProducts.add(
        InvoiceProduct.builder()
            .id(randomUUID().toString())
            .idInvoice(invoiceId)
            .createdAt(Instant.now())
            .description(subscriptionProduct == null ? invoiceTitle : subscriptionProduct.getName())
            .quantity(1)
            .unitPrice(
                parseFraction(
                    subscriptionProduct == null
                        ? 4900
                        : subscriptionProduct.getPriceInCents().doubleValue()))
            .vatPercent(new Fraction(BigInteger.valueOf(2000)))
            .status(ProductStatus.ENABLED)
            .build());

    // TODO: add variable product when it 's computed correctly
    return invoiceProducts;
  }
}
