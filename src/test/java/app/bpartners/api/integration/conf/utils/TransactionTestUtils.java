package app.bpartners.api.integration.conf.utils;

import app.bpartners.api.endpoint.rest.model.MonthlyTransactionsSummary;
import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.endpoint.rest.model.TransactionsSummary;
import app.bpartners.api.repository.bridge.model.Transaction.BridgeTransaction;
import app.bpartners.api.repository.jpa.model.HTransaction;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static java.util.Calendar.DECEMBER;
import static java.util.Calendar.JANUARY;

public class TransactionTestUtils {
  private TransactionTestUtils() {
  }

  public static BridgeTransaction bridgeTransaction1() {
    return BridgeTransaction.builder()
        .id(1L)
        .label("Transaction 1")
        .amount(100.0)
        .transactionDate(LocalDate.of(2023, 1, 1))
        .build();
  }

  public static BridgeTransaction bridgeTransaction2() {
    return BridgeTransaction.builder()
        .id(2L)
        .label("Transaction 2")
        .amount(200.0)
        .transactionDate(LocalDate.of(2023, 1, 2))
        .build();
  }

  public static BridgeTransaction bridgeTransaction3() {
    return BridgeTransaction.builder()
        .id(3L)
        .label("Transaction 3")
        .amount(300.0)
        .transactionDate(LocalDate.of(2023, 1, 3))
        .build();
  }

  public static HTransaction jpaTransactionEntity1() {
    return HTransaction.builder()
        .id("transaction1_id")
        .idAccount(JOE_DOE_ACCOUNT_ID)
        .idBridge(null)
        .label("Création de site vitrine")
        .reference("REF_001")
        .amount("50000/1")
        .currency("EUR")
        .side("CREDIT_SIDE")
        .status(TransactionStatus.PENDING)
        .paymentDateTime(Instant.parse("2022-08-26T06:33:50.595Z"))
        .build();
  }

  public static HTransaction jpaTransactionEntity2() {
    return HTransaction.builder()
        .id("transaction2_id")
        .idAccount(JOE_DOE_ACCOUNT_ID)
        .idBridge(null)
        .label("Premier virement")
        .reference("JOE-001")
        .amount("50000/1")
        .currency("EUR")
        .side("CREDIT_SIDE")
        .status(TransactionStatus.BOOKED)
        .paymentDateTime(Instant.parse("2022-08-24T03:39:33.315Z"))
        .build();
  }

  public static HTransaction transactionEntity1() {
    return HTransaction.builder()
        .id("transaction1_id")
        .idAccount(JOE_DOE_ACCOUNT_ID)
        .idBridge(bridgeTransaction1().getId())
        .label(bridgeTransaction1().getLabel())
        .amount(String.valueOf(parseFraction(bridgeTransaction2().getAmount())))
        .currency(bridgeTransaction1().getCurrency())
        .side(bridgeTransaction1().getSide())
        .status(bridgeTransaction1().getStatus())
        .paymentDateTime(bridgeTransaction1().getTransactionDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant())
        .build();
  }

  public static HTransaction transactionEntity2() {
    return HTransaction.builder()
        .id("transaction2_id")
        .idAccount(JOE_DOE_ACCOUNT_ID)
        .idBridge(bridgeTransaction2().getId())
        .label(bridgeTransaction2().getLabel())
        .amount(String.valueOf(parseFraction(bridgeTransaction2().getAmount())))
        .currency(bridgeTransaction2().getCurrency())
        .side(bridgeTransaction2().getSide())
        .status(bridgeTransaction2().getStatus())
        .paymentDateTime(bridgeTransaction2().getTransactionDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()).build();
  }

  public static HTransaction bridgeTransactionEntity3() {
    return HTransaction.builder()
        .id("bridge_transaction3_id")
        .idAccount(JOE_DOE_ACCOUNT_ID)
        .idBridge(bridgeTransaction3().getId())
        .label(bridgeTransaction3().getLabel())
        .amount(String.valueOf(parseFraction(bridgeTransaction3().getAmount())))
        .currency(bridgeTransaction3().getCurrency())
        .side(bridgeTransaction3().getSide())
        .status(bridgeTransaction3().getStatus())
        .paymentDateTime(bridgeTransaction3().getTransactionDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant())
        .build();
  }

  public static MonthlyTransactionsSummary month1() {
    return new MonthlyTransactionsSummary()
        .id("monthly_transactions_summary1_id")
        .month(JANUARY)
        .income(0)
        .outcome(0)
        .cashFlow(10000);
  }

  public static MonthlyTransactionsSummary month2() {
    return new MonthlyTransactionsSummary()
        .id("monthly_transactions_summary2_id")
        .month(DECEMBER)
        .income(0)
        .outcome(0)
        .cashFlow(10000);
  }

  public static TransactionsSummary transactionsSummary1() {
    int annualIncome = month1().getIncome() + month2().getIncome();
    int annualOutcome = month1().getOutcome() + month2().getOutcome();
    return new TransactionsSummary()
        .year(LocalDate.now().getYear())
        .annualIncome(annualIncome)
        .annualOutcome(annualOutcome)
        .annualCashFlow(10000)
        .summary(List.of(month1(), month2()));
  }

  public static List<MonthlyTransactionsSummary> ignoreUpdatedAt(
      List<MonthlyTransactionsSummary> actual) {
    actual.forEach(monthlyTransactionsSummary -> {
      monthlyTransactionsSummary.setUpdatedAt(null);
    });
    return actual;
  }
}
