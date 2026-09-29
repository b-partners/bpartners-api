package app.bpartners.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionInvoicePeriod;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.repository.jpa.SubscriptionInvoicePeriodRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import java.sql.Timestamp;
import java.time.Instant;
import javax.sql.DataSource;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class SubscriptionInvoicePeriodIT extends MockedThirdParties {
  private static final String SUBSCRIBER_ID = "subscriber_id";
  private static final String COMMITMENT_INVOICE_ID = "it_commitment_invoice_id";
  private static final String SINGLE_DATE_INVOICE_ID = "it_single_date_invoice_id";
  private static final String COMMITMENT_PAYMENT_ID = "it_commitment_payment_id";
  private static final String SINGLE_DATE_PAYMENT_ID = "it_single_date_payment_id";
  private static final String MAPPED_PAYMENT_ID = "it_mapped_payment_id";
  private static final String MAPPED_INVOICE_ID = "it_mapped_invoice_id";

  @Autowired private SubscriptionPaymentRepository subscriptionPaymentRepository;
  @Autowired private SubscriptionInvoicePeriodRepository subscriptionInvoicePeriodRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private DataSource dataSource;

  @AfterEach
  void cleanUp() {
    jdbcTemplate.update(
        "delete from subscription_invoice_period where invoice_id in (?, ?, ?)",
        COMMITMENT_INVOICE_ID,
        SINGLE_DATE_INVOICE_ID,
        MAPPED_INVOICE_ID);
    jdbcTemplate.update(
        "delete from subscription_payment where id in (?, ?, ?)",
        COMMITMENT_PAYMENT_ID,
        SINGLE_DATE_PAYMENT_ID,
        MAPPED_PAYMENT_ID);
    jdbcTemplate.update(
        "delete from \"invoice\" where id in (?, ?)",
        COMMITMENT_INVOICE_ID,
        SINGLE_DATE_INVOICE_ID);
  }

  @Test
  void the_migrated_columns_and_table_carry_the_invoiced_period() {
    var invoicedPeriodStart = Instant.parse("2026-10-01T00:00:00Z");
    var invoicedPeriodEnd = Instant.parse("2027-08-31T00:00:00Z");
    subscriptionPaymentRepository.save(
        SubscriptionPayment.builder()
            .id(MAPPED_PAYMENT_ID)
            .userId(SUBSCRIBER_ID)
            .stripeInvoiceId("in_it_mapped")
            .billingInterval(BillingInterval.MONTHLY)
            .invoiceId(MAPPED_INVOICE_ID)
            .invoicedPeriodStartDatetime(invoicedPeriodStart)
            .invoicedPeriodEndDatetime(invoicedPeriodEnd)
            .build());
    subscriptionInvoicePeriodRepository.save(
        SubscriptionInvoicePeriod.builder()
            .id("it_mapped_period_id")
            .userId(SUBSCRIBER_ID)
            .invoiceId(MAPPED_INVOICE_ID)
            .periodStartDatetime(invoicedPeriodStart)
            .periodEndDatetime(invoicedPeriodEnd)
            .build());

    var persistedPayment = subscriptionPaymentRepository.findById(MAPPED_PAYMENT_ID).orElseThrow();
    var persistedPeriod =
        subscriptionInvoicePeriodRepository.findByInvoiceId(MAPPED_INVOICE_ID).orElseThrow();
    assertEquals(invoicedPeriodStart, persistedPayment.getInvoicedPeriodStartDatetime());
    assertEquals(invoicedPeriodEnd, persistedPayment.getInvoicedPeriodEndDatetime());
    assertEquals(SUBSCRIBER_ID, persistedPeriod.getUserId());
    assertEquals(invoicedPeriodStart, persistedPeriod.getPeriodStartDatetime());
    assertEquals(invoicedPeriodEnd, persistedPeriod.getPeriodEndDatetime());
    assertTrue(
        subscriptionInvoicePeriodRepository.findByUserId(SUBSCRIBER_ID).stream()
            .anyMatch(period -> MAPPED_INVOICE_ID.equals(period.getInvoiceId())));
  }

  @Test
  void the_scripts_backfill_the_period_covered_by_the_existing_subscription_invoices() {
    givenSubscriptionInvoice(
        COMMITMENT_INVOICE_ID,
        "IT-COMMITMENT",
        "Facture d'abonnement pour la période du 01/09/2026 au 31/08/2027");
    givenSubscriptionInvoice(
        SINGLE_DATE_INVOICE_ID, "IT-SINGLE-DATE", "Facture d'abonnement du 04/03/2026");
    givenPaidSubscriptionPayment(COMMITMENT_PAYMENT_ID, "in_it_commitment", COMMITMENT_INVOICE_ID);
    givenPaidSubscriptionPayment(
        SINGLE_DATE_PAYMENT_ID, "in_it_single_date", SINGLE_DATE_INVOICE_ID);

    runInvoicedPeriodMigrations();

    var backfilledPayment =
        subscriptionPaymentRepository.findById(COMMITMENT_PAYMENT_ID).orElseThrow();
    assertEquals(
        Instant.parse("2026-09-01T00:00:00Z"), backfilledPayment.getInvoicedPeriodStartDatetime());
    assertEquals(
        Instant.parse("2027-08-31T00:00:00Z"), backfilledPayment.getInvoicedPeriodEndDatetime());
    var invoicedPeriod =
        subscriptionInvoicePeriodRepository.findByInvoiceId(COMMITMENT_INVOICE_ID).orElseThrow();
    assertEquals(SUBSCRIBER_ID, invoicedPeriod.getUserId());
    assertEquals(Instant.parse("2026-09-01T00:00:00Z"), invoicedPeriod.getPeriodStartDatetime());
    assertEquals(Instant.parse("2027-08-31T00:00:00Z"), invoicedPeriod.getPeriodEndDatetime());

    var untouchedPayment =
        subscriptionPaymentRepository.findById(SINGLE_DATE_PAYMENT_ID).orElseThrow();
    assertNull(untouchedPayment.getInvoicedPeriodStartDatetime());
    assertNull(untouchedPayment.getInvoicedPeriodEndDatetime());
    assertTrue(
        subscriptionInvoicePeriodRepository.findByInvoiceId(SINGLE_DATE_INVOICE_ID).isEmpty());
  }

  @Test
  void the_scripts_can_be_replayed_without_duplicating_the_covered_periods() {
    givenSubscriptionInvoice(
        COMMITMENT_INVOICE_ID,
        "IT-COMMITMENT",
        "Facture d'abonnement pour la période du 01/09/2026 au 31/08/2027");
    givenPaidSubscriptionPayment(COMMITMENT_PAYMENT_ID, "in_it_commitment", COMMITMENT_INVOICE_ID);

    runInvoicedPeriodMigrations();
    runInvoicedPeriodMigrations();

    assertEquals(
        1,
        jdbcTemplate.queryForObject(
            "select count(*) from subscription_invoice_period where invoice_id = ?",
            Integer.class,
            COMMITMENT_INVOICE_ID));
  }

  @SneakyThrows
  private void runInvoicedPeriodMigrations() {
    try (var connection = dataSource.getConnection()) {
      ScriptUtils.executeSqlScript(
          connection,
          new ClassPathResource(
              "db/migration/V43_132__Add_subscription_payment_invoiced_period.sql"));
      ScriptUtils.executeSqlScript(
          connection,
          new ClassPathResource("db/migration/V43_133__Create_subscription_invoice_period.sql"));
    }
  }

  private void givenSubscriptionInvoice(String invoiceId, String reference, String title) {
    jdbcTemplate.update(
        "insert into \"invoice\" (id, id_user, title, \"ref\", id_customer, sending_date,"
            + " to_pay_at, status, archive_status, created_datetime, payment_type)"
            + " values (?, 'user_to_credit_id', ?, ?, 'subscription_customer_id', '2026-09-01',"
            + " '2026-09-01', 'CONFIRMED'::invoice_status, 'ENABLED'::archive_status,"
            + " '2026-09-01T10:00:00.00Z', 'CASH'::payment_type)",
        invoiceId,
        title,
        reference);
  }

  private void givenPaidSubscriptionPayment(
      String paymentId, String stripeInvoiceId, String invoiceId) {
    jdbcTemplate.update(
        "insert into subscription_payment (id, user_id, stripe_invoice_id, billing_interval,"
            + " label, amount_in_cents_without_vat, amount_in_cents_with_vat, vat_percent,"
            + " period_start_datetime, period_end_datetime, payment_datetime, invoice_id)"
            + " values (?, ?, ?, 'MONTHLY'::billing_interval, 'Essentiel', 4083, 4900, 2000,"
            + " ?, ?, ?, ?)",
        paymentId,
        SUBSCRIBER_ID,
        stripeInvoiceId,
        Timestamp.from(Instant.parse("2026-09-01T09:30:00Z")),
        Timestamp.from(Instant.parse("2026-09-30T09:30:00Z")),
        Timestamp.from(Instant.parse("2026-09-01T09:30:00Z")),
        invoiceId);
  }
}
