package app.bpartners.api.unit;

import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.DEBIT;
import static app.bpartners.api.model.credit.CreditTransactionType.CONSUMPTION;
import static app.bpartners.api.model.credit.CreditTransactionType.PURCHASE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.InsufficientCreditsException;
import app.bpartners.api.model.validator.CreditTransactionValidator;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.service.credit.CreditLedgerService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CreditLedgerServiceTest {
  CreditTransactionRepository creditTransactionRepository = mock(CreditTransactionRepository.class);
  CreditLedgerService subject =
      new CreditLedgerService(creditTransactionRepository, new CreditTransactionValidator());

  CreditLedgerServiceTest() {
    when(creditTransactionRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void append_credit_generates_id_and_datetime_then_saves() {
    when(creditTransactionRepository.findAllByUserId("user_id")).thenReturn(List.of());

    var actual =
        subject.append(
            CreditTransaction.builder()
                .userId("user_id")
                .type(PURCHASE)
                .movementType(CREDIT)
                .credits(30L)
                .build());

    assertNotNull(actual.getId());
    assertNotNull(actual.getCreationDatetime());
    assertEquals(30L, actual.getCredits());
    var inOrder = Mockito.inOrder(creditTransactionRepository);
    inOrder.verify(creditTransactionRepository).acquireWalletLock("user_id");
    inOrder.verify(creditTransactionRepository).findAllByUserId("user_id");
    inOrder.verify(creditTransactionRepository).save(any());
  }

  @Test
  void append_debit_within_balance_is_saved() {
    when(creditTransactionRepository.findAllByUserId("user_id"))
        .thenReturn(List.of(CreditTransaction.builder().movementType(CREDIT).credits(30L).build()));

    var actual =
        subject.append(
            CreditTransaction.builder()
                .userId("user_id")
                .type(CONSUMPTION)
                .movementType(DEBIT)
                .credits(5L)
                .build());

    assertEquals(5L, actual.getCredits());
    verify(creditTransactionRepository).save(any());
  }

  @Test
  void append_debit_over_balance_throws_insufficient_credits() {
    when(creditTransactionRepository.findAllByUserId("user_id"))
        .thenReturn(List.of(CreditTransaction.builder().movementType(CREDIT).credits(3L).build()));

    var draft =
        CreditTransaction.builder()
            .userId("user_id")
            .type(CONSUMPTION)
            .movementType(DEBIT)
            .credits(5L)
            .build();

    var exception = assertThrows(InsufficientCreditsException.class, () -> subject.append(draft));

    assertEquals(5L, exception.getRequiredCredits());
    assertEquals(3L, exception.getAvailableCredits());
    verify(creditTransactionRepository, never()).save(any());
  }

  @Test
  void append_rejects_non_positive_credits() {
    var draft =
        CreditTransaction.builder()
            .userId("user_id")
            .type(PURCHASE)
            .movementType(CREDIT)
            .credits(0L)
            .build();

    assertThrows(BadRequestException.class, () -> subject.append(draft));
  }

  @Test
  void append_debit_takes_previous_debits_into_account() {
    when(creditTransactionRepository.findAllByUserId("user_id"))
        .thenReturn(
            List.of(
                CreditTransaction.builder().movementType(CREDIT).credits(10L).build(),
                CreditTransaction.builder().movementType(DEBIT).credits(8L).build()));

    var draft =
        CreditTransaction.builder()
            .userId("user_id")
            .type(CONSUMPTION)
            .movementType(DEBIT)
            .credits(5L)
            .build();

    var exception = assertThrows(InsufficientCreditsException.class, () -> subject.append(draft));

    assertEquals(2L, exception.getAvailableCredits());
    verify(creditTransactionRepository, never()).save(any());
  }

  @Test
  void append_debit_draining_the_whole_balance_is_saved() {
    when(creditTransactionRepository.findAllByUserId("user_id"))
        .thenReturn(
            List.of(
                CreditTransaction.builder().movementType(CREDIT).credits(10L).build(),
                CreditTransaction.builder().movementType(DEBIT).credits(8L).build()));

    var actual =
        subject.append(
            CreditTransaction.builder()
                .userId("user_id")
                .type(CONSUMPTION)
                .movementType(DEBIT)
                .credits(2L)
                .build());

    assertEquals(2L, actual.getCredits());
    verify(creditTransactionRepository).save(any());
  }

  @Test
  void append_preserves_provided_id_and_creation_datetime() {
    var creationDatetime = Instant.parse("2025-01-01T10:00:00Z");
    when(creditTransactionRepository.findAllByUserId("user_id")).thenReturn(List.of());

    var actual =
        subject.append(
            CreditTransaction.builder()
                .id("provided_id")
                .userId("user_id")
                .type(PURCHASE)
                .movementType(CREDIT)
                .credits(30L)
                .creationDatetime(creationDatetime)
                .build());

    assertEquals("provided_id", actual.getId());
    assertEquals(creationDatetime, actual.getCreationDatetime());
  }
}
