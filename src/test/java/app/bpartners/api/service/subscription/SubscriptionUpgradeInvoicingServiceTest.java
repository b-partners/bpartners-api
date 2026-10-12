package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.INVOICED;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PAID;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionInvoicePeriodRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionUpgradeJpaRepository;
import app.bpartners.api.service.customer.SubscriptionCustomerResolver;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionUpgradeInvoicingServiceTest {
  private static final String UPGRADE_ID = "upgrade_id";
  private static final String USER_TO_CREDIT_ID = "birdia_id";
  private static final String USER_ID = "subscriber_id";

  UserSubscriptionUpgradeJpaRepository upgradeRepository = mock();
  SubscriptionProductRepository subscriptionProductRepository = mock();
  SubscriptionInvoicePeriodRepository subscriptionInvoicePeriodRepository = mock();
  UserRepository userRepository = mock();
  SubscriptionCustomerResolver subscriptionCustomerResolver = mock();
  InvoiceService invoiceService = mock();
  InvoiceRepository invoiceRepository = mock();
  SubscriptionInvoiceMailer subscriptionInvoiceMailer = mock();
  CustomDateFormatter customDateFormatter = new CustomDateFormatter();

  SubscriptionUpgradeInvoicingService subject =
      new SubscriptionUpgradeInvoicingService(
          upgradeRepository,
          subscriptionProductRepository,
          subscriptionInvoicePeriodRepository,
          userRepository,
          new UserSubscriptionConf(USER_TO_CREDIT_ID),
          subscriptionCustomerResolver,
          invoiceService,
          invoiceRepository,
          customDateFormatter,
          new SubscriptionUpgradeInvoiceLines(customDateFormatter),
          subscriptionInvoiceMailer);

  SubscriptionProduct essential = product("essential_id", "Essentiel", 5_000L);
  SubscriptionProduct pro = product("pro_id", "Pro", 10_000L);

  @BeforeEach
  void setUp() {
    when(subscriptionProductRepository.findById("essential_id")).thenReturn(Optional.of(essential));
    when(subscriptionProductRepository.findById("pro_id")).thenReturn(Optional.of(pro));
    when(userRepository.getById(USER_TO_CREDIT_ID))
        .thenReturn(User.builder().id(USER_TO_CREDIT_ID).build());
    when(userRepository.getById(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
    when(subscriptionCustomerResolver.apply(any(), any())).thenReturn(Customer.builder().build());
    when(invoiceService.crupdateSubscriptionInvoice(any(Invoice.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(invoiceRepository.findByIdUserAndRef(anyString(), anyString())).thenReturn(List.of());
    when(subscriptionInvoicePeriodRepository.findByUserId(USER_ID)).thenReturn(List.of());
    when(upgradeRepository.save(any(UserSubscriptionUpgrade.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void issues_a_negative_credit_note_then_the_new_invoice_and_flags_the_upgrade_invoiced() {
    when(upgradeRepository.findById(UPGRADE_ID)).thenReturn(Optional.of(upgrade(PAID)));

    var actual = subject.invoice(UPGRADE_ID).orElseThrow();

    assertEquals(INVOICED, actual.getStatus());
    assertNotNull(actual.getCreditNoteInvoiceId());
    assertNotNull(actual.getNewInvoiceId());
    var invoices = ArgumentCaptor.forClass(Invoice.class);
    verify(invoiceService, times(2)).crupdateSubscriptionInvoice(invoices.capture());
    var creditNote = invoices.getAllValues().get(0);
    var newInvoice = invoices.getAllValues().get(1);
    assertTrue(creditNote.getTotalPriceWithVat().getApproximatedValue() < 0);
    assertTrue(creditNote.getTitle().startsWith("Avoir sur l'abonnement Essentiel"));
    assertTrue(newInvoice.getTotalPriceWithVat().getApproximatedValue() > 0);
    assertEquals(creditNote.getId(), actual.getCreditNoteInvoiceId());
    assertEquals(newInvoice.getId(), actual.getNewInvoiceId());
    verify(subscriptionInvoicePeriodRepository).save(any());
  }

  @Test
  void issues_only_the_missing_new_invoice_when_the_credit_note_already_exists() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(
            Optional.of(upgrade(PAID).toBuilder().creditNoteInvoiceId("credit_note_id").build()));

    var actual = subject.invoice(UPGRADE_ID).orElseThrow();

    verify(invoiceService, times(1)).crupdateSubscriptionInvoice(any(Invoice.class));
    assertEquals("credit_note_id", actual.getCreditNoteInvoiceId());
    assertEquals(INVOICED, actual.getStatus());
  }

  @Test
  void skips_an_already_invoiced_upgrade() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(
            Optional.of(
                upgrade(INVOICED).toBuilder()
                    .creditNoteInvoiceId("credit_note_id")
                    .newInvoiceId("new_invoice_id")
                    .build()));

    assertTrue(subject.invoice(UPGRADE_ID).isEmpty());

    verifyNoInteractions(invoiceService);
  }

  @Test
  void skips_a_pending_upgrade() {
    when(upgradeRepository.findById(UPGRADE_ID)).thenReturn(Optional.of(upgrade(PENDING)));

    assertTrue(subject.invoice(UPGRADE_ID).isEmpty());

    verify(invoiceService, never()).crupdateSubscriptionInvoice(any());
  }

  private static UserSubscriptionUpgrade upgrade(
      app.bpartners.api.model.subscription.SubscriptionUpgradeStatus status) {
    return UserSubscriptionUpgrade.builder()
        .id(UPGRADE_ID)
        .userId(USER_ID)
        .fromSubscriptionProductId("essential_id")
        .toSubscriptionProductId("pro_id")
        .fromBillingInterval(MONTHLY)
        .toBillingInterval(MONTHLY)
        .upgradeDatetime(Instant.parse("2026-10-11T22:00:00Z"))
        .oldCoverageEndDatetime(Instant.parse("2027-09-29T22:00:00Z"))
        .newCommitmentEndDatetime(Instant.parse("2027-10-11T22:00:00Z"))
        .prorationAmountInCentsWithVat(3_871L)
        .status(status)
        .build();
  }

  private static SubscriptionProduct product(String id, String name, long priceInCentsWithoutVat) {
    return SubscriptionProduct.builder()
        .id(id)
        .name(name)
        .vatPercent(2_000L)
        .priceInCentsWithoutVat(priceInCentsWithoutVat)
        .build();
  }
}
