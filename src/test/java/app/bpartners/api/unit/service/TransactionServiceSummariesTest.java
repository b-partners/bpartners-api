package app.bpartners.api.unit.service;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Money;
import app.bpartners.api.model.MonthlyTransactionsSummary;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.repository.TransactionRepository;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import app.bpartners.api.service.AccountService;
import app.bpartners.api.service.TransactionService;
import java.math.BigInteger;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static app.bpartners.api.integration.conf.TestUtils.CREDIT_SIDE;
import static app.bpartners.api.integration.conf.TestUtils.DEBIT_SIDE;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionServiceSummariesTest {
  TransactionService transactionService;
  TransactionRepository transactionRepository;
  TransactionsSummaryRepository transactionsSummaryRepository;
  AccountService accountService;

  private static Account joeDoeAccount() {
    return Account.builder()
        .userId(JOE_DOE_ID)
        .availableBalance(new Money())
        .build();
  }

  @BeforeEach
  void setUp() {
    transactionsSummaryRepository = mock(TransactionsSummaryRepository.class);
    transactionRepository = mock(TransactionRepository.class);
    accountService = mock(AccountService.class);
    transactionService = new TransactionService(
        transactionRepository,
        transactionsSummaryRepository,
        accountService
    );

    when(transactionRepository.findByAccountIdAndStatusBetweenInstants(any(), any(), any(), any()))
        .thenReturn(transactions());
  }

  @Test
  void refresh_summaries_with_last_month_summary() {
    YearMonth lastMonth = YearMonth.now().minusMonths(1);
    Instant now = Instant.now();
    when(transactionsSummaryRepository.getByIdUserAndYearMonth(
        JOE_DOE_ID,
        lastMonth.getYear(),
        lastMonth.getMonthValue() - 1
    )).thenReturn(lastMonthlySummary(now));
    ArgumentCaptor<String> idUserCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> yearCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<MonthlyTransactionsSummary> summaryCaptor =
        ArgumentCaptor.forClass(MonthlyTransactionsSummary.class);

    transactionService.refreshMonthSummary(joeDoeAccount(),
        YearMonth.now(),
        transactions());
    verify(transactionsSummaryRepository).updateYearMonthSummary(
        idUserCaptor.capture(),
        yearCaptor.capture(),
        summaryCaptor.capture()
    );
    assertEquals(JOE_DOE_ID, idUserCaptor.getValue());
    assertEquals(YearMonth.now().getYear(), yearCaptor.getValue());
    assertEquals(refreshedSummary1(summaryCaptor.getValue().getUpdatedAt()),
        summaryCaptor.getValue());
  }

  @Test
  void refresh_summaries_without_last_month_summary() {
    YearMonth lastMonth = YearMonth.now().minusMonths(1);
    when(transactionsSummaryRepository.getByIdUserAndYearMonth(
        JOE_DOE_ID,
        lastMonth.getYear(),
        lastMonth.getMonthValue() - 1
    )).thenReturn(null);
    ArgumentCaptor<String> idUserCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> yearCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<MonthlyTransactionsSummary> summaryCaptor =
        ArgumentCaptor.forClass(MonthlyTransactionsSummary.class);

    transactionService.refreshMonthSummary(joeDoeAccount(), YearMonth.now(), transactions());
    verify(transactionsSummaryRepository).updateYearMonthSummary(
        idUserCaptor.capture(),
        yearCaptor.capture(),
        summaryCaptor.capture()
    );
    assertEquals(JOE_DOE_ID, idUserCaptor.getValue());
    assertEquals(YearMonth.now().getYear(), yearCaptor.getValue());
    assertEquals(refreshedSummary2(summaryCaptor.getValue().getUpdatedAt()),
        summaryCaptor.getValue());
  }

  private Transaction.TransactionBuilder transactionWith100Value() {
    return Transaction
        .builder()
        .amount(new Fraction(BigInteger.valueOf(100)));
  }

  private MonthlyTransactionsSummary lastMonthlySummary(Instant updatedAt) {
    return MonthlyTransactionsSummary
        .builder()
        .cashFlow(new Fraction(BigInteger.TWO))
        .updatedAt(updatedAt)
        .build();
  }

  private MonthlyTransactionsSummary refreshedSummary1(Instant updatedAt) {
    return MonthlyTransactionsSummary
        .builder()
        .income(new Fraction(BigInteger.valueOf(100)))
        .month(YearMonth.now().getMonthValue() - 1)
        .outcome(new Fraction(BigInteger.valueOf(100)))
        .cashFlow(new Fraction())
        .updatedAt(updatedAt)
        .build();
  }

  private MonthlyTransactionsSummary refreshedSummary2(Instant updatedAt) {
    return MonthlyTransactionsSummary
        .builder()
        .month(YearMonth.now().getMonthValue() - 1)
        .income(new Fraction(BigInteger.valueOf(100)))
        .outcome(new Fraction(BigInteger.valueOf(100)))
        .cashFlow(new Fraction())
        .updatedAt(updatedAt)
        .build();
  }

  private List<Transaction> transactions() {
    Instant now = Instant.now();
    return List.of(
        transactionWith100Value()
            .side(CREDIT_SIDE)
            .paymentDatetime(now)
            .build(),
        transactionWith100Value()
            .side(DEBIT_SIDE)
            .paymentDatetime(now)
            .build()
    );
  }
}
