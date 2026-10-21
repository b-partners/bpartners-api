package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.INVOICED;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PAID;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.subscription.SubscriptionInvoicePeriod;
import app.bpartners.api.model.subscription.SubscriptionUpgradeStatus;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.SubscriptionInvoicePeriodRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionUpgradeJpaRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LiveSubscriptionInvoicePeriodFinderTest {
  private static final String USER_ID = "subscriber_id";
  private static final Instant FIRST_INVOICE_CREATION = Instant.parse("2026-10-14T09:22:00Z");
  private static final Instant FIRST_UPGRADE_CREATION = Instant.parse("2026-10-14T09:25:00Z");
  private static final Instant FIRST_UPGRADE_INVOICE_CREATION =
      Instant.parse("2026-10-14T09:25:30Z");
  private static final Instant SECOND_UPGRADE_CREATION = Instant.parse("2026-10-14T09:28:00Z");
  private static final Instant NOW = Instant.parse("2026-10-14T09:30:00Z");

  SubscriptionInvoicePeriodRepository periodRepository = mock();
  SubscriptionPaymentRepository paymentRepository = mock();
  UserSubscriptionUpgradeJpaRepository upgradeRepository = mock();
  InvoiceRepository invoiceRepository = mock();

  LiveSubscriptionInvoicePeriodFinder subject =
      new LiveSubscriptionInvoicePeriodFinder(
          periodRepository, paymentRepository, upgradeRepository, invoiceRepository);

  @BeforeEach
  void setUp() {
    when(paymentRepository.findRefundedInvoiceIdsByUserId(USER_ID)).thenReturn(List.of());
    when(upgradeRepository.findAllByUserIdOrderByCreationDatetimeDesc(USER_ID))
        .thenReturn(List.of());
    when(periodRepository.findByUserId(USER_ID))
        .thenReturn(List.of(period("essential_invoice"), period("pro_invoice")));
    when(invoiceRepository.findById("essential_invoice"))
        .thenReturn(
            Invoice.builder().id("essential_invoice").createdAt(FIRST_INVOICE_CREATION).build());
    when(invoiceRepository.findById("pro_invoice"))
        .thenReturn(
            Invoice.builder().id("pro_invoice").createdAt(FIRST_UPGRADE_INVOICE_CREATION).build());
  }

  @Test
  void keeps_every_period_when_the_user_never_upgraded() {
    assertEquals(
        List.of("essential_invoice", "pro_invoice"), invoiceIdsOf(subject.findLive(USER_ID, NOW)));
  }

  @Test
  void drops_the_periods_of_refunded_invoices() {
    when(paymentRepository.findRefundedInvoiceIdsByUserId(USER_ID))
        .thenReturn(List.of("essential_invoice"));

    assertEquals(List.of("pro_invoice"), invoiceIdsOf(subject.findLive(USER_ID, NOW)));
  }

  @Test
  void drops_the_periods_of_invoices_issued_before_a_paid_upgrade() {
    givenUpgrades(upgrade(PAID, FIRST_UPGRADE_CREATION));

    assertEquals(List.of("pro_invoice"), invoiceIdsOf(subject.findLive(USER_ID, NOW)));
  }

  @Test
  void drops_the_periods_of_invoices_issued_before_an_invoiced_upgrade() {
    givenUpgrades(upgrade(INVOICED, FIRST_UPGRADE_CREATION));

    assertEquals(List.of("pro_invoice"), invoiceIdsOf(subject.findLive(USER_ID, NOW)));
  }

  @Test
  void drops_the_invoice_of_an_upgrade_once_a_later_upgrade_supersedes_it() {
    givenUpgrades(
        upgrade(PAID, SECOND_UPGRADE_CREATION), upgrade(INVOICED, FIRST_UPGRADE_CREATION));

    assertEquals(List.of(), invoiceIdsOf(subject.findLive(USER_ID, NOW)));
  }

  @Test
  void ignores_the_upgrade_being_processed_and_the_later_ones() {
    givenUpgrades(
        upgrade(PAID, SECOND_UPGRADE_CREATION), upgrade(INVOICED, FIRST_UPGRADE_CREATION));

    assertEquals(
        List.of("pro_invoice"), invoiceIdsOf(subject.findLive(USER_ID, SECOND_UPGRADE_CREATION)));
  }

  @Test
  void ignores_an_upgrade_that_is_not_paid_yet() {
    givenUpgrades(upgrade(PENDING, FIRST_UPGRADE_CREATION));

    assertEquals(
        List.of("essential_invoice", "pro_invoice"), invoiceIdsOf(subject.findLive(USER_ID, NOW)));
  }

  @Test
  void keeps_a_period_whose_invoice_cannot_be_dated() {
    givenUpgrades(upgrade(PAID, FIRST_UPGRADE_CREATION));
    when(invoiceRepository.findById("essential_invoice"))
        .thenReturn(Invoice.builder().id("essential_invoice").build());

    assertEquals(
        List.of("essential_invoice", "pro_invoice"), invoiceIdsOf(subject.findLive(USER_ID, NOW)));
  }

  @Test
  void ignores_an_upgrade_without_creation_datetime() {
    givenUpgrades(upgrade(PAID, null));

    assertEquals(
        List.of("essential_invoice", "pro_invoice"), invoiceIdsOf(subject.findLive(USER_ID, NOW)));
  }

  private void givenUpgrades(UserSubscriptionUpgrade... upgrades) {
    when(upgradeRepository.findAllByUserIdOrderByCreationDatetimeDesc(USER_ID))
        .thenReturn(List.of(upgrades));
  }

  private static List<String> invoiceIdsOf(List<SubscriptionInvoicePeriod> periods) {
    return periods.stream().map(SubscriptionInvoicePeriod::getInvoiceId).toList();
  }

  private static SubscriptionInvoicePeriod period(String invoiceId) {
    return SubscriptionInvoicePeriod.builder()
        .id("period_" + invoiceId)
        .userId(USER_ID)
        .invoiceId(invoiceId)
        .build();
  }

  private static UserSubscriptionUpgrade upgrade(
      SubscriptionUpgradeStatus status, Instant creation) {
    return UserSubscriptionUpgrade.builder()
        .id("upgrade_" + status + creation)
        .userId(USER_ID)
        .status(status)
        .creationDatetime(creation)
        .build();
  }
}
