package app.bpartners.api.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.service.subscription.SubscriptionInvoiceService;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
class SubscriptionInvoiceServiceIT extends MockedThirdParties {
  private static final String USER_TO_CREDIT_ID = "user_to_credit_id";
  private static final String SUBSCRIBER_ID = "subscriber_id";
  private static final String STRIPE_SUBSCRIBER_ID = "stripe_subscriber_id";
  private static final YearMonth MARCH_2024 = YearMonth.of(2024, 3);

  @Autowired SubscriptionInvoiceService subject;

  @BeforeEach
  void setUp() {
    when(userSubscriptionConf.getUserToCreditId()).thenReturn(USER_TO_CREDIT_ID);
  }

  @Test
  void filters_on_customer_email_month_and_title_ok() {
    var actual = subject.getSubscriptionInvoices(SUBSCRIBER_ID, MARCH_2024);

    // every other seeded invoice differs by exactly one criterion: month, customer, status
    // or archive status, so a single match proves each predicate is applied conjunctively
    assertEquals(List.of("subscription_invoice_march_id"), idsOf(actual));
  }

  @Test
  void falls_back_on_stripe_correspondence_email_ok() {
    // this user email matches no customer: only the correspondence email does
    var actual = subject.getSubscriptionInvoices(STRIPE_SUBSCRIBER_ID, MARCH_2024);

    assertEquals(List.of("stripe_subscription_invoice_march_id"), idsOf(actual));
  }

  @Test
  void returns_empty_for_a_month_without_subscription_invoice_ok() {
    var actual = subject.getSubscriptionInvoices(SUBSCRIBER_ID, YearMonth.of(2024, 4));

    assertTrue(actual.isEmpty());
  }

  private static List<String> idsOf(List<Invoice> invoices) {
    return invoices.stream().map(Invoice::getId).toList();
  }
}
