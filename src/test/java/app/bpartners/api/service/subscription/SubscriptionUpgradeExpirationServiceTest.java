package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.AWAITING_PAYMENT;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PAID;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.subscription.SubscriptionUpgradeStatus;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.repository.jpa.UserSubscriptionUpgradeJpaRepository;
import app.bpartners.api.service.subscription.SubscriptionUpgradeTransitionService.Expiration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SubscriptionUpgradeExpirationServiceTest {
  UserSubscriptionUpgradeJpaRepository upgradeRepository = mock();
  SubscriptionUpgradeTransitionService transitionService = mock();
  SubscriptionUpgradeCompletionService completionService = mock();
  SubscriptionUpgradeExpirationService subject =
      new SubscriptionUpgradeExpirationService(
          upgradeRepository, transitionService, completionService);

  @Test
  void expires_the_overdue_upgrades_awaiting_their_payment_and_counts_them() {
    var first = upgrade("first_id", AWAITING_PAYMENT, -60);
    var second = upgrade("second_id", AWAITING_PAYMENT, -60);
    var alreadyPaid = upgrade("paid_id", PAID, -60);
    when(upgradeRepository.findAllByPaymentExpirationDatetimeBefore(any(Instant.class)))
        .thenReturn(List.of(first, alreadyPaid, second));
    when(transitionService.expire("first_id")).thenReturn(Expiration.EXPIRED);
    when(transitionService.expire("second_id")).thenReturn(Expiration.SKIPPED);

    assertEquals(1, subject.expireOverdue());

    verify(transitionService, never()).expire("paid_id");
  }

  @Test
  void expires_nothing_when_no_upgrade_is_overdue() {
    when(upgradeRepository.findAllByPaymentExpirationDatetimeBefore(any(Instant.class)))
        .thenReturn(List.of());

    assertEquals(0, subject.expireOverdue());

    verifyNoInteractions(transitionService, completionService);
  }

  @Test
  void completes_the_upgrade_whose_invoice_was_paid_at_the_deadline() {
    var upgrade = upgrade("upgrade_id", AWAITING_PAYMENT, -60);
    when(transitionService.expire("upgrade_id")).thenReturn(Expiration.PAID);

    assertFalse(subject.expireIfOverdue(upgrade));

    verify(completionService).completeAfterPayment("upgrade_id", "in_upgrade_id");
  }

  @Test
  void leaves_an_upgrade_still_inside_its_payment_window() {
    assertFalse(subject.expireIfOverdue(upgrade("upgrade_id", AWAITING_PAYMENT, 600)));

    verifyNoInteractions(transitionService, completionService);
  }

  @Test
  void leaves_an_upgrade_that_is_not_awaiting_its_payment() {
    assertFalse(subject.expireIfOverdue(upgrade("upgrade_id", PENDING, -60)));

    verifyNoInteractions(transitionService, completionService);
  }

  @Test
  void reports_an_expired_upgrade() {
    when(transitionService.expire("upgrade_id")).thenReturn(Expiration.EXPIRED);

    assertTrue(subject.expireIfOverdue(upgrade("upgrade_id", AWAITING_PAYMENT, -60)));
  }

  @Test
  void keeps_the_upgrade_when_it_can_not_be_expired() {
    when(transitionService.expire("upgrade_id"))
        .thenThrow(new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, "stripe down"));

    assertFalse(subject.expireIfOverdue(upgrade("upgrade_id", AWAITING_PAYMENT, -60)));
  }

  @Test
  void expires_the_overdue_quotes_and_counts_them_with_the_expired_payments() {
    var overdueQuote =
        upgrade("quote_id", PENDING, 0).toBuilder()
            .quoteExpirationDatetime(Instant.now().minusSeconds(60))
            .build();
    var processing = upgrade("processing_id", PAID, 0);
    when(upgradeRepository.findAllByQuoteExpirationDatetimeBefore(any(Instant.class)))
        .thenReturn(List.of(overdueQuote, processing));
    when(transitionService.expireQuote("quote_id")).thenReturn(true);

    assertEquals(1, subject.expireOverdue());

    verify(transitionService, never()).expireQuote("processing_id");
  }

  @Test
  void expires_a_quote_past_its_deadline() {
    var quote = upgrade("quote_id", PENDING, 0).toBuilder().quoteExpirationDatetime(Instant.now().minusSeconds(60)).build();
    when(transitionService.expireQuote("quote_id")).thenReturn(true);

    assertTrue(subject.expireQuoteIfOverdue(quote));
  }

  @Test
  void leaves_a_quote_inside_its_validity_or_an_upgrade_that_is_not_pending() {
    var valid = upgrade("quote_id", PENDING, 0).toBuilder().quoteExpirationDatetime(Instant.now().plusSeconds(600)).build();
    var paid = upgrade("paid_id", PAID, 0).toBuilder().quoteExpirationDatetime(Instant.now().minusSeconds(60)).build();

    assertFalse(subject.expireQuoteIfOverdue(valid));
    assertFalse(subject.expireQuoteIfOverdue(paid));

    verifyNoInteractions(transitionService);
  }

  private static UserSubscriptionUpgrade upgrade(
      String id, SubscriptionUpgradeStatus status, long expiresInSeconds) {
    return UserSubscriptionUpgrade.builder()
        .id(id)
        .stripeProrationInvoiceId("in_" + id)
        .paymentExpirationDatetime(Instant.now().plusSeconds(expiresInSeconds))
        .status(status)
        .build();
  }
}
