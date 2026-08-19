package app.bpartners.api.unit;

import static app.bpartners.api.model.credit.CreditCode.ANALYSES_10;
import static app.bpartners.api.model.credit.CreditCode.PACK_CUSTOM;
import static app.bpartners.api.model.credit.CreditPurchaseOrigin.SELF_SERVICE;
import static app.bpartners.api.model.credit.CreditPurchaseStatus.COMPLETED;
import static app.bpartners.api.model.credit.CreditPurchaseStatus.PENDING;
import static app.bpartners.api.model.credit.CreditPurchaseType.CUSTOM;
import static app.bpartners.api.model.credit.CreditPurchaseType.PACK;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionType.PURCHASE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.User;
import app.bpartners.api.model.credit.CreditPack;
import app.bpartners.api.model.credit.CreditPurchase;
import app.bpartners.api.model.credit.CreditPurchaseCharge;
import app.bpartners.api.model.credit.CreditPurchaseSubmission;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.credit.CreditUnitPrice;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.ConflictException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.jpa.CreditPurchaseRepository;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.service.credit.CreditLedgerService;
import app.bpartners.api.service.credit.CreditPurchaseService;
import app.bpartners.api.service.credit.CreditService;
import app.bpartners.api.service.subscription.StripeCreditPurchaseService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreditPurchaseServiceTest {
  CreditPurchaseRepository creditPurchaseRepository = mock(CreditPurchaseRepository.class);
  CreditTransactionRepository creditTransactionRepository = mock(CreditTransactionRepository.class);
  CreditService creditService = mock(CreditService.class);
  CreditLedgerService creditLedgerService = mock(CreditLedgerService.class);
  StripeCreditPurchaseService stripeCreditPurchaseService = mock(StripeCreditPurchaseService.class);

  CreditPurchaseService subject =
      new CreditPurchaseService(
          creditPurchaseRepository,
          creditTransactionRepository,
          creditService,
          creditLedgerService,
          stripeCreditPurchaseService);

  User user = User.builder().id("user_id").userSubscriptionId("cus_1").build();
  User userWithCard =
      User.builder().id("user_id").userSubscriptionId("cus_1").paymentMethodExists(true).build();

  CreditPurchaseServiceTest() {
    when(creditService.resolveCreditUnitPrice(user)).thenReturn(new CreditUnitPrice(400L, 2000L));
    when(creditPurchaseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(stripeCreditPurchaseService.checkoutSessionUrl(any(), any(), any(), any()))
        .thenReturn("https://pay.stripe.com/session");
    when(creditService.resolveCreditUnitPrice(userWithCard))
        .thenReturn(new CreditUnitPrice(400L, 2000L));
    when(creditLedgerService.append(any()))
        .thenAnswer(
            invocation ->
                ((CreditTransaction) invocation.getArgument(0)).toBuilder().id("tx_1").build());
    when(creditTransactionRepository.findFirstByCreditPurchaseId(any()))
        .thenReturn(Optional.empty());
  }

  private CreditPack analyses10Pack() {
    return CreditPack.builder()
        .id("pack_10")
        .code(ANALYSES_10)
        .description("10 analyses de toiture")
        .creditPurchaseType(PACK)
        .credits(10L)
        .build();
  }

  private CreditPurchaseSubmission packSubmission(Integer quantity) {
    return new CreditPurchaseSubmission(
        "purchase_1",
        PACK,
        "pack_10",
        quantity,
        null,
        "https://birdia.fr/success",
        "https://birdia.fr/failure");
  }

  private CreditPurchaseSubmission customSubmission(Long credits) {
    return new CreditPurchaseSubmission(
        "purchase_1",
        CUSTOM,
        null,
        null,
        credits,
        "https://birdia.fr/success",
        "https://birdia.fr/failure");
  }

  @Test
  void submit_pack_purchase_is_priced_and_persisted_pending() {
    when(creditPurchaseRepository.findById("purchase_1")).thenReturn(Optional.empty());
    when(creditService.getCreditPack("pack_10")).thenReturn(analyses10Pack());
    var captor = ArgumentCaptor.forClass(CreditPurchase.class);

    var actual = subject.submit(user, packSubmission(2));

    verify(creditPurchaseRepository).save(captor.capture());
    assertEquals(captor.getValue(), actual);
    assertEquals("purchase_1", actual.getId());
    assertEquals("user_id", actual.getUserId());
    assertEquals(PACK, actual.getType());
    assertEquals("pack_10", actual.getCreditPack().getId());
    assertEquals(2, actual.getQuantity());
    assertEquals(20L, actual.getCredits());
    assertEquals(400L, actual.getCreditUnitPriceInCentsWithoutVat());
    assertEquals(8000L, actual.getAmountInCentsWithoutVat());
    assertEquals(9600L, actual.getAmountInCentsWithVat());
    assertEquals(2000L, actual.getVatPercent());
    assertEquals(PENDING, actual.getStatus());
    assertEquals(SELF_SERVICE, actual.getOrigin());
    assertEquals("https://pay.stripe.com/session", actual.getRedirectionUrl());
    assertEquals("https://birdia.fr/success", actual.getRedirectionSuccessUrl());
    assertEquals("https://birdia.fr/failure", actual.getRedirectionFailureUrl());
    assertNotNull(actual.getCreationDatetime());
    assertNull(actual.getCompletionDatetime());
    assertNull(actual.getCreditTransactionId());
    assertNull(actual.getCreditsExpirationDatetime());
  }

  @Test
  void submit_pack_purchase_defaults_quantity_to_one() {
    when(creditPurchaseRepository.findById("purchase_1")).thenReturn(Optional.empty());
    when(creditService.getCreditPack("pack_10")).thenReturn(analyses10Pack());

    var actual = subject.submit(user, packSubmission(null));

    assertEquals(1, actual.getQuantity());
    assertEquals(10L, actual.getCredits());
    assertEquals(4000L, actual.getAmountInCentsWithoutVat());
    assertEquals(4800L, actual.getAmountInCentsWithVat());
  }

  @Test
  void submit_custom_purchase_prices_the_chosen_amount() {
    when(creditPurchaseRepository.findById("purchase_1")).thenReturn(Optional.empty());

    var actual = subject.submit(user, customSubmission(7L));

    assertEquals(CUSTOM, actual.getType());
    assertNull(actual.getCreditPack());
    assertNull(actual.getQuantity());
    assertEquals(7L, actual.getCredits());
    assertEquals(2800L, actual.getAmountInCentsWithoutVat());
    assertEquals(3360L, actual.getAmountInCentsWithVat());
  }

  @Test
  void resubmitting_the_same_pack_payload_returns_the_existing_purchase() {
    var existing =
        CreditPurchase.builder()
            .id("purchase_1")
            .userId("user_id")
            .type(PACK)
            .creditPack(analyses10Pack())
            .quantity(2)
            .credits(20L)
            .status(PENDING)
            .build();
    when(creditPurchaseRepository.findById("purchase_1")).thenReturn(Optional.of(existing));

    var actual = subject.submit(user, packSubmission(2));

    assertEquals(existing, actual);
    verify(creditPurchaseRepository, never()).save(any());
    verify(stripeCreditPurchaseService, never()).checkoutSessionUrl(any(), any(), any(), any());
  }

  @Test
  void resubmitting_the_same_custom_payload_returns_the_existing_purchase() {
    var existing =
        CreditPurchase.builder()
            .id("purchase_1")
            .userId("user_id")
            .type(CUSTOM)
            .credits(7L)
            .status(PENDING)
            .build();
    when(creditPurchaseRepository.findById("purchase_1")).thenReturn(Optional.of(existing));

    assertEquals(existing, subject.submit(user, customSubmission(7L)));
    verify(creditPurchaseRepository, never()).save(any());
  }

  @Test
  void resubmitting_another_quantity_is_a_conflict() {
    when(creditPurchaseRepository.findById("purchase_1"))
        .thenReturn(
            Optional.of(
                CreditPurchase.builder()
                    .id("purchase_1")
                    .userId("user_id")
                    .type(PACK)
                    .creditPack(analyses10Pack())
                    .quantity(1)
                    .build()));

    var exception =
        assertThrows(ConflictException.class, () -> subject.submit(user, packSubmission(2)));

    assertEquals(
        "CreditPurchase.id=purchase_1 was already submitted with a different payload,"
            + " a purchase is immutable",
        exception.getMessage());
    verify(creditPurchaseRepository, never()).save(any());
  }

  @Test
  void resubmitting_another_pack_is_a_conflict() {
    when(creditPurchaseRepository.findById("purchase_1"))
        .thenReturn(
            Optional.of(
                CreditPurchase.builder()
                    .id("purchase_1")
                    .userId("user_id")
                    .type(PACK)
                    .creditPack(CreditPack.builder().id("pack_20").build())
                    .quantity(2)
                    .build()));

    assertThrows(ConflictException.class, () -> subject.submit(user, packSubmission(2)));
  }

  @Test
  void resubmitting_a_pack_purchase_over_a_pack_less_row_is_a_conflict() {
    when(creditPurchaseRepository.findById("purchase_1"))
        .thenReturn(
            Optional.of(
                CreditPurchase.builder().id("purchase_1").userId("user_id").type(PACK).build()));

    assertThrows(ConflictException.class, () -> subject.submit(user, packSubmission(2)));
  }

  @Test
  void resubmitting_another_type_is_a_conflict() {
    when(creditPurchaseRepository.findById("purchase_1"))
        .thenReturn(
            Optional.of(
                CreditPurchase.builder()
                    .id("purchase_1")
                    .userId("user_id")
                    .type(CUSTOM)
                    .credits(20L)
                    .build()));

    assertThrows(ConflictException.class, () -> subject.submit(user, packSubmission(2)));
  }

  @Test
  void resubmitting_another_credits_amount_is_a_conflict() {
    when(creditPurchaseRepository.findById("purchase_1"))
        .thenReturn(
            Optional.of(
                CreditPurchase.builder()
                    .id("purchase_1")
                    .userId("user_id")
                    .type(CUSTOM)
                    .credits(9L)
                    .build()));

    assertThrows(ConflictException.class, () -> subject.submit(user, customSubmission(7L)));
  }

  @Test
  void resubmitting_a_purchase_identifier_of_another_user_is_a_conflict() {
    when(creditPurchaseRepository.findById("purchase_1"))
        .thenReturn(
            Optional.of(
                CreditPurchase.builder()
                    .id("purchase_1")
                    .userId("another_user_id")
                    .type(PACK)
                    .creditPack(analyses10Pack())
                    .quantity(2)
                    .build()));

    var exception =
        assertThrows(ConflictException.class, () -> subject.submit(user, packSubmission(2)));

    assertEquals("CreditPurchase.id=purchase_1 already exists", exception.getMessage());
  }

  @Test
  void submit_rejects_a_deprecated_pack() {
    when(creditPurchaseRepository.findById("purchase_1")).thenReturn(Optional.empty());
    when(creditService.getCreditPack("pack_10"))
        .thenReturn(analyses10Pack().toBuilder().deprecated(true).build());

    var exception =
        assertThrows(BadRequestException.class, () -> subject.submit(user, packSubmission(1)));

    assertEquals(
        "CreditPack(id=pack_10) is deprecated and can not be purchased", exception.getMessage());
    verify(creditPurchaseRepository, never()).save(any());
  }

  @Test
  void submit_rejects_a_pack_carrying_no_fixed_credits() {
    when(creditPurchaseRepository.findById("purchase_1")).thenReturn(Optional.empty());
    when(creditService.getCreditPack("pack_10"))
        .thenReturn(
            CreditPack.builder()
                .id("pack_10")
                .code(PACK_CUSTOM)
                .creditPurchaseType(CUSTOM)
                .build());

    var exception =
        assertThrows(BadRequestException.class, () -> subject.submit(user, packSubmission(1)));

    assertEquals(
        "CreditPack(id=pack_10) carries no fixed credits amount,"
            + " submit a CUSTOM purchase instead",
        exception.getMessage());
  }

  @Test
  void submit_propagates_an_unknown_pack_as_not_found() {
    when(creditPurchaseRepository.findById("purchase_1")).thenReturn(Optional.empty());
    when(creditService.getCreditPack("pack_10"))
        .thenThrow(new NotFoundException("CreditPack(id=pack_10) not found"));

    assertThrows(NotFoundException.class, () -> subject.submit(user, packSubmission(1)));
  }

  @Test
  void submit_rejects_a_user_without_stripe_customer() {
    when(creditPurchaseRepository.findById("purchase_1")).thenReturn(Optional.empty());

    var exception =
        assertThrows(
            BadRequestException.class,
            () -> subject.submit(User.builder().id("user_id").build(), customSubmission(7L)));

    assertEquals(
        "User.id=user_id is not associated to a stripe customer yet", exception.getMessage());
    verify(creditPurchaseRepository, never()).save(any());
  }

  @Test
  void resubmitting_over_a_row_without_quantity_treats_it_as_one() {
    var existing =
        CreditPurchase.builder()
            .id("purchase_1")
            .userId("user_id")
            .type(PACK)
            .creditPack(analyses10Pack())
            .credits(10L)
            .build();
    when(creditPurchaseRepository.findById("purchase_1")).thenReturn(Optional.of(existing));

    assertEquals(existing, subject.submit(user, packSubmission(1)));
  }

  @Test
  void submit_charges_the_registered_card_and_completes_the_purchase() {
    when(creditPurchaseRepository.findById("purchase_1")).thenReturn(Optional.empty());
    when(stripeCreditPurchaseService.chargeOffSession(eq("cus_1"), any()))
        .thenReturn(CreditPurchaseCharge.succeeded("pi_1"));
    var transactionCaptor = ArgumentCaptor.forClass(CreditTransaction.class);

    var actual = subject.submit(userWithCard, customSubmission(7L));

    assertEquals(COMPLETED, actual.getStatus());
    assertEquals("tx_1", actual.getCreditTransactionId());
    assertNotNull(actual.getCompletionDatetime());
    assertNull(actual.getRedirectionUrl());
    verify(stripeCreditPurchaseService, never()).checkoutSessionUrl(any(), any(), any(), any());
    verify(creditLedgerService).append(transactionCaptor.capture());
    var appended = transactionCaptor.getValue();
    assertEquals("user_id", appended.getUserId());
    assertEquals(PURCHASE, appended.getType());
    assertEquals(CREDIT, appended.getMovementType());
    assertEquals(7L, appended.getCredits());
    assertEquals("7 crédits d'analyse", appended.getLabel());
    assertEquals("purchase_1", appended.getCreditPurchaseId());
    assertNull(appended.getExpirationDatetime());
  }

  @Test
  void submit_falls_back_to_checkout_when_the_card_needs_authentication() {
    when(creditPurchaseRepository.findById("purchase_1")).thenReturn(Optional.empty());
    when(stripeCreditPurchaseService.chargeOffSession(eq("cus_1"), any()))
        .thenReturn(CreditPurchaseCharge.failed("authentication_required"));

    var actual = subject.submit(userWithCard, customSubmission(7L));

    assertEquals(PENDING, actual.getStatus());
    assertEquals("https://pay.stripe.com/session", actual.getRedirectionUrl());
    assertNull(actual.getCreditTransactionId());
    verify(creditLedgerService, never()).append(any());
  }

  @Test
  void submit_does_not_try_to_charge_when_no_payment_method_is_registered() {
    when(creditPurchaseRepository.findById("purchase_1")).thenReturn(Optional.empty());

    var actual = subject.submit(user, customSubmission(7L));

    assertEquals(PENDING, actual.getStatus());
    assertEquals("https://pay.stripe.com/session", actual.getRedirectionUrl());
    verify(stripeCreditPurchaseService, never()).chargeOffSession(any(), any());
    verify(creditLedgerService, never()).append(any());
  }

  @Test
  void complete_grants_the_credits_and_marks_the_purchase_completed() {
    when(creditPurchaseRepository.findById("purchase_1"))
        .thenReturn(
            Optional.of(
                CreditPurchase.builder()
                    .id("purchase_1")
                    .userId("user_id")
                    .type(CUSTOM)
                    .credits(7L)
                    .status(PENDING)
                    .build()));

    var actual = subject.complete("purchase_1").orElseThrow();

    assertEquals(COMPLETED, actual.getStatus());
    assertEquals("tx_1", actual.getCreditTransactionId());
    assertNotNull(actual.getCompletionDatetime());
    verify(creditLedgerService, times(1)).append(any());
  }

  @Test
  void complete_an_already_completed_purchase_does_not_grant_twice() {
    var completed =
        CreditPurchase.builder()
            .id("purchase_1")
            .userId("user_id")
            .type(CUSTOM)
            .credits(7L)
            .status(COMPLETED)
            .creditTransactionId("tx_1")
            .build();
    when(creditPurchaseRepository.findById("purchase_1")).thenReturn(Optional.of(completed));

    assertEquals(completed, subject.complete("purchase_1").orElseThrow());

    verify(creditLedgerService, never()).append(any());
    verify(creditPurchaseRepository, never()).save(any());
  }

  @Test
  void complete_reuses_the_transaction_already_appended_for_the_purchase() {
    when(creditPurchaseRepository.findById("purchase_1"))
        .thenReturn(
            Optional.of(
                CreditPurchase.builder()
                    .id("purchase_1")
                    .userId("user_id")
                    .type(CUSTOM)
                    .credits(7L)
                    .status(PENDING)
                    .build()));
    when(creditTransactionRepository.findFirstByCreditPurchaseId("purchase_1"))
        .thenReturn(Optional.of(CreditTransaction.builder().id("tx_already").build()));

    var actual = subject.complete("purchase_1").orElseThrow();

    assertEquals("tx_already", actual.getCreditTransactionId());
    assertEquals(COMPLETED, actual.getStatus());
    verify(creditLedgerService, never()).append(any());
  }

  @Test
  void complete_keeps_the_first_completion_datetime() {
    var completionDatetime = java.time.Instant.parse("2026-08-01T00:00:00Z");
    when(creditPurchaseRepository.findById("purchase_1"))
        .thenReturn(
            Optional.of(
                CreditPurchase.builder()
                    .id("purchase_1")
                    .userId("user_id")
                    .type(CUSTOM)
                    .credits(7L)
                    .status(PENDING)
                    .completionDatetime(completionDatetime)
                    .build()));

    assertEquals(
        completionDatetime, subject.complete("purchase_1").orElseThrow().getCompletionDatetime());
  }

  @Test
  void complete_an_unknown_purchase_returns_empty() {
    when(creditPurchaseRepository.findById("unknown")).thenReturn(Optional.empty());

    assertTrue(subject.complete("unknown").isEmpty());
    verify(creditLedgerService, never()).append(any());
  }

  @Test
  void complete_a_purchase_marked_completed_without_transaction_grants_the_credits() {
    when(creditPurchaseRepository.findById("purchase_1"))
        .thenReturn(
            Optional.of(
                CreditPurchase.builder()
                    .id("purchase_1")
                    .userId("user_id")
                    .type(CUSTOM)
                    .credits(7L)
                    .status(COMPLETED)
                    .build()));

    var actual = subject.complete("purchase_1").orElseThrow();

    assertEquals("tx_1", actual.getCreditTransactionId());
    verify(creditLedgerService, times(1)).append(any());
  }
}
