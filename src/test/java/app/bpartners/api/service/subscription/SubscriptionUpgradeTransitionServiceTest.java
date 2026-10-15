package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.ABANDONED;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.AWAITING_PAYMENT;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.EXPIRED;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PAID;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PROCESSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.repository.jpa.UserSubscriptionUpgradeJpaRepository;
import app.bpartners.api.service.subscription.SubscriptionUpgradeTransitionService.Abandonment;
import app.bpartners.api.service.subscription.SubscriptionUpgradeTransitionService.Expiration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionUpgradeTransitionServiceTest {
  private static final String UPGRADE_ID = "upgrade_id";
  private static final String INVOICE_ID = "in_open";

  UserSubscriptionUpgradeJpaRepository upgradeRepository = mock();
  StripeUpgradeService stripeUpgradeService = mock();
  SubscriptionUpgradeTransitionService subject =
      new SubscriptionUpgradeTransitionService(upgradeRepository, stripeUpgradeService);

  @BeforeEach
  void setUp() {
    when(upgradeRepository.save(any(UserSubscriptionUpgrade.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void claims_an_upgrade_awaiting_the_paid_invoice_by_moving_it_to_processing() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(Optional.of(upgrade(AWAITING_PAYMENT, Instant.now().plusSeconds(60))));

    var claimed = subject.claimPaid(UPGRADE_ID, INVOICE_ID);

    assertEquals(PROCESSING, claimed.orElseThrow().getStatus());
    var order = inOrder(upgradeRepository);
    order.verify(upgradeRepository).acquireLock(UPGRADE_ID);
    order.verify(upgradeRepository).findById(UPGRADE_ID);
    order.verify(upgradeRepository).save(any(UserSubscriptionUpgrade.class));
  }

  @Test
  void does_not_claim_an_upgrade_that_is_not_awaiting_its_payment() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(Optional.of(upgrade(PROCESSING, null)))
        .thenReturn(Optional.of(upgrade(PAID, null)))
        .thenReturn(Optional.of(upgrade(EXPIRED, null)));

    assertTrue(subject.claimPaid(UPGRADE_ID, INVOICE_ID).isEmpty());
    assertTrue(subject.claimPaid(UPGRADE_ID, INVOICE_ID).isEmpty());
    assertTrue(subject.claimPaid(UPGRADE_ID, INVOICE_ID).isEmpty());

    verify(upgradeRepository, never()).save(any());
  }

  @Test
  void does_not_claim_an_upgrade_for_the_payment_of_another_invoice() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(Optional.of(upgrade(AWAITING_PAYMENT, Instant.now().plusSeconds(60))));

    assertTrue(subject.claimPaid(UPGRADE_ID, "in_other").isEmpty());

    verify(upgradeRepository, never()).save(any());
  }

  @Test
  void does_not_claim_an_unknown_upgrade() {
    when(upgradeRepository.findById(UPGRADE_ID)).thenReturn(Optional.empty());

    assertTrue(subject.claimPaid(UPGRADE_ID, INVOICE_ID).isEmpty());
  }

  @Test
  void expires_an_overdue_upgrade_once_its_invoice_is_voided() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(Optional.of(upgrade(AWAITING_PAYMENT, Instant.now().minusSeconds(60))));
    when(stripeUpgradeService.voidOpenInvoice(INVOICE_ID))
        .thenReturn(StripeInvoiceVoidOutcome.VOIDED);

    assertEquals(Expiration.EXPIRED, subject.expire(UPGRADE_ID));

    var saved = ArgumentCaptor.forClass(UserSubscriptionUpgrade.class);
    verify(upgradeRepository).save(saved.capture());
    assertEquals(EXPIRED, saved.getValue().getStatus());
    var order = inOrder(upgradeRepository, stripeUpgradeService);
    order.verify(upgradeRepository).acquireLock(UPGRADE_ID);
    order.verify(stripeUpgradeService).voidOpenInvoice(INVOICE_ID);
    order.verify(upgradeRepository).save(any(UserSubscriptionUpgrade.class));
  }

  @Test
  void keeps_an_overdue_upgrade_whose_invoice_was_paid_at_the_deadline() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(Optional.of(upgrade(AWAITING_PAYMENT, Instant.now().minusSeconds(60))));
    when(stripeUpgradeService.voidOpenInvoice(INVOICE_ID))
        .thenReturn(StripeInvoiceVoidOutcome.PAID);

    assertEquals(Expiration.PAID, subject.expire(UPGRADE_ID));

    verify(upgradeRepository, never()).save(any());
  }

  @Test
  void does_not_expire_an_upgrade_still_inside_its_payment_window() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(Optional.of(upgrade(AWAITING_PAYMENT, Instant.now().plusSeconds(600))));

    assertEquals(Expiration.SKIPPED, subject.expire(UPGRADE_ID));

    verifyNoInteractions(stripeUpgradeService);
  }

  @Test
  void does_not_expire_an_upgrade_that_is_not_awaiting_its_payment_or_is_unknown() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(Optional.of(upgrade(PROCESSING, Instant.now().minusSeconds(60))))
        .thenReturn(Optional.empty());

    assertEquals(Expiration.SKIPPED, subject.expire(UPGRADE_ID));
    assertEquals(Expiration.SKIPPED, subject.expire(UPGRADE_ID));

    verifyNoInteractions(stripeUpgradeService);
    verify(upgradeRepository, never()).save(any());
  }

  @Test
  void lets_the_failure_of_the_void_surface_without_expiring_the_upgrade() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(Optional.of(upgrade(AWAITING_PAYMENT, Instant.now().minusSeconds(60))));
    when(stripeUpgradeService.voidOpenInvoice(INVOICE_ID))
        .thenThrow(new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, "stripe down"));

    assertThrows(ApiException.class, () -> subject.expire(UPGRADE_ID));

    verify(upgradeRepository, never()).save(any());
  }

  @Test
  void abandons_an_upgrade_inside_its_payment_window_once_its_invoice_is_voided() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(Optional.of(upgrade(AWAITING_PAYMENT, Instant.now().plusSeconds(600))));
    when(stripeUpgradeService.voidOpenInvoice(INVOICE_ID))
        .thenReturn(StripeInvoiceVoidOutcome.VOIDED);

    assertEquals(Abandonment.ABANDONED, subject.abandon(UPGRADE_ID));

    var saved = ArgumentCaptor.forClass(UserSubscriptionUpgrade.class);
    verify(upgradeRepository).save(saved.capture());
    assertEquals(ABANDONED, saved.getValue().getStatus());
    var order = inOrder(upgradeRepository, stripeUpgradeService);
    order.verify(upgradeRepository).acquireLock(UPGRADE_ID);
    order.verify(stripeUpgradeService).voidOpenInvoice(INVOICE_ID);
    order.verify(upgradeRepository).save(any(UserSubscriptionUpgrade.class));
  }

  @Test
  void keeps_an_upgrade_whose_invoice_was_paid_before_its_abandonment() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(Optional.of(upgrade(AWAITING_PAYMENT, Instant.now().plusSeconds(600))));
    when(stripeUpgradeService.voidOpenInvoice(INVOICE_ID))
        .thenReturn(StripeInvoiceVoidOutcome.PAID);

    assertEquals(Abandonment.PAID, subject.abandon(UPGRADE_ID));

    verify(upgradeRepository, never()).save(any());
  }

  @Test
  void does_not_abandon_an_overdue_upgrade() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(Optional.of(upgrade(AWAITING_PAYMENT, Instant.now().minusSeconds(60))));

    assertEquals(Abandonment.NOT_ABANDONABLE, subject.abandon(UPGRADE_ID));

    verifyNoInteractions(stripeUpgradeService);
    verify(upgradeRepository, never()).save(any());
  }

  @Test
  void does_not_abandon_an_upgrade_that_is_not_awaiting_its_payment_or_is_unknown() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(Optional.of(upgrade(PAID, Instant.now().plusSeconds(600))))
        .thenReturn(Optional.of(upgrade(EXPIRED, Instant.now().plusSeconds(600))))
        .thenReturn(Optional.empty());

    assertEquals(Abandonment.NOT_ABANDONABLE, subject.abandon(UPGRADE_ID));
    assertEquals(Abandonment.NOT_ABANDONABLE, subject.abandon(UPGRADE_ID));
    assertEquals(Abandonment.NOT_ABANDONABLE, subject.abandon(UPGRADE_ID));

    verifyNoInteractions(stripeUpgradeService);
    verify(upgradeRepository, never()).save(any());
  }

  @Test
  void lets_the_failure_of_the_void_surface_without_abandoning_the_upgrade() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(Optional.of(upgrade(AWAITING_PAYMENT, Instant.now().plusSeconds(600))));
    when(stripeUpgradeService.voidOpenInvoice(INVOICE_ID))
        .thenThrow(new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, "stripe down"));

    assertThrows(ApiException.class, () -> subject.abandon(UPGRADE_ID));

    verify(upgradeRepository, never()).save(any());
  }

  @Test
  void confirms_a_quote_inside_its_validity_by_moving_it_to_processing() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(Optional.of(quote(PENDING, Instant.now().plusSeconds(600))));

    var confirmed = subject.confirm(UPGRADE_ID).orElseThrow();

    assertEquals(PROCESSING, confirmed.getStatus());
    assertTrue(confirmed.getConfirmationDatetime() != null);
    var order = inOrder(upgradeRepository);
    order.verify(upgradeRepository).acquireLock(UPGRADE_ID);
    order.verify(upgradeRepository).save(any(UserSubscriptionUpgrade.class));
  }

  @Test
  void does_not_confirm_an_expired_quote_nor_an_upgrade_that_is_not_pending() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(Optional.of(quote(PENDING, Instant.now().minusSeconds(60))))
        .thenReturn(Optional.of(quote(PAID, Instant.now().plusSeconds(600))))
        .thenReturn(Optional.empty());

    assertTrue(subject.confirm(UPGRADE_ID).isEmpty());
    assertTrue(subject.confirm(UPGRADE_ID).isEmpty());
    assertTrue(subject.confirm(UPGRADE_ID).isEmpty());

    verify(upgradeRepository, never()).save(any());
  }

  @Test
  void expires_an_overdue_quote() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(Optional.of(quote(PENDING, Instant.now().minusSeconds(60))));

    assertTrue(subject.expireQuote(UPGRADE_ID));

    var saved = ArgumentCaptor.forClass(UserSubscriptionUpgrade.class);
    verify(upgradeRepository).save(saved.capture());
    assertEquals(EXPIRED, saved.getValue().getStatus());
    verifyNoInteractions(stripeUpgradeService);
  }

  @Test
  void does_not_expire_a_quote_still_valid_or_an_upgrade_that_is_not_pending() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(Optional.of(quote(PENDING, Instant.now().plusSeconds(600))))
        .thenReturn(Optional.of(quote(PROCESSING, Instant.now().minusSeconds(60))))
        .thenReturn(Optional.empty());

    assertFalse(subject.expireQuote(UPGRADE_ID));
    assertFalse(subject.expireQuote(UPGRADE_ID));
    assertFalse(subject.expireQuote(UPGRADE_ID));

    verify(upgradeRepository, never()).save(any());
  }

  @Test
  void abandons_a_quote_inside_its_validity_without_touching_stripe() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(Optional.of(quote(PENDING, Instant.now().plusSeconds(600))));

    assertEquals(Abandonment.ABANDONED, subject.abandon(UPGRADE_ID));

    var saved = ArgumentCaptor.forClass(UserSubscriptionUpgrade.class);
    verify(upgradeRepository).save(saved.capture());
    assertEquals(ABANDONED, saved.getValue().getStatus());
    verifyNoInteractions(stripeUpgradeService);
  }

  @Test
  void does_not_abandon_an_expired_quote() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(Optional.of(quote(PENDING, Instant.now().minusSeconds(60))));

    assertEquals(Abandonment.NOT_ABANDONABLE, subject.abandon(UPGRADE_ID));

    verify(upgradeRepository, never()).save(any());
  }

  private static UserSubscriptionUpgrade quote(
      app.bpartners.api.model.subscription.SubscriptionUpgradeStatus status,
      Instant quoteExpiration) {
    return UserSubscriptionUpgrade.builder()
        .id(UPGRADE_ID)
        .quoteExpirationDatetime(quoteExpiration)
        .status(status)
        .build();
  }

  private static UserSubscriptionUpgrade upgrade(
      app.bpartners.api.model.subscription.SubscriptionUpgradeStatus status,
      Instant paymentExpiration) {
    return UserSubscriptionUpgrade.builder()
        .id(UPGRADE_ID)
        .stripeProrationInvoiceId(INVOICE_ID)
        .paymentExpirationDatetime(paymentExpiration)
        .status(status)
        .build();
  }
}
