package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.model.credit.CreditAdjustmentReason.MIGRATION;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.DEBIT;
import static app.bpartners.api.model.credit.CreditTransactionType.ADJUSTMENT;
import static app.bpartners.api.model.credit.CreditTransactionType.CONSUMPTION;
import static app.bpartners.api.model.credit.CreditTransactionType.PURCHASE;
import static app.bpartners.api.model.credit.CreditTransactionType.SUBSCRIPTION_GRANT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.credit.CreditTransactionMovementType;
import app.bpartners.api.model.credit.CreditTransactionType;
import app.bpartners.api.model.exception.InsufficientCreditsException;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.service.credit.CreditLedgerService;
import app.bpartners.api.service.credit.CreditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CreditLedgerIT extends MockedThirdParties {
  @Autowired private CreditLedgerService creditLedgerService;
  @Autowired private CreditService creditService;
  @Autowired private CreditTransactionRepository creditTransactionRepository;

  private long spendableCredits() {
    return creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits();
  }

  private CreditTransaction append(
      CreditTransactionType type, CreditTransactionMovementType movementType, long credits) {
    return creditLedgerService.append(
        CreditTransaction.builder()
            .userId(JOE_DOE_ID)
            .type(type)
            .movementType(movementType)
            .credits(credits)
            .build());
  }

  @BeforeEach
  void setUp() {
    creditTransactionRepository.deleteAll();
  }

  @Test
  void append_maintains_running_balance_across_movements() {
    append(SUBSCRIPTION_GRANT, CREDIT, 20L);
    append(PURCHASE, CREDIT, 30L);
    append(CONSUMPTION, DEBIT, 5L);

    assertEquals(45L, spendableCredits());
    assertEquals(3, creditTransactionRepository.findAllByUserId(JOE_DOE_ID).size());
  }

  @Test
  void append_generates_id_and_creation_datetime() {
    var appended = append(PURCHASE, CREDIT, 30L);

    var persisted = creditTransactionRepository.findById(appended.getId()).orElseThrow();
    assertNotNull(persisted.getId());
    assertNotNull(persisted.getCreationDatetime());
    assertEquals(30L, persisted.getCredits());
  }

  @Test
  void append_debit_over_balance_is_rejected_and_persists_nothing() {
    append(PURCHASE, CREDIT, 10L);

    assertThrows(InsufficientCreditsException.class, () -> append(CONSUMPTION, DEBIT, 100L));

    assertEquals(10L, spendableCredits());
    assertEquals(1, creditTransactionRepository.findAllByUserId(JOE_DOE_ID).size());
  }

  @Test
  void append_round_trips_adjustment_reason() {
    var appended =
        creditLedgerService.append(
            CreditTransaction.builder()
                .userId(JOE_DOE_ID)
                .type(ADJUSTMENT)
                .movementType(CREDIT)
                .credits(15L)
                .adjustmentReason(MIGRATION)
                .label("Reprise migration")
                .build());

    var persisted = creditTransactionRepository.findById(appended.getId()).orElseThrow();
    assertEquals(MIGRATION, persisted.getAdjustmentReason());
    assertEquals("Reprise migration", persisted.getLabel());
  }
}
