package app.bpartners.api.unit.service;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.MonthlyTransactionsSummary;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.repository.TransactionRepository;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
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
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionServiceSummariesTest {
  TransactionService transactionService;
  TransactionRepository transactionRepository;
  AccountHolderJpaRepository accountHolderJpaRepository;
  TransactionsSummaryRepository transactionsSummaryRepository;
  InvoiceJpaRepository invoiceRepositoryMock;

  @BeforeEach
  void setUp() {
    accountHolderJpaRepository = mock(AccountHolderJpaRepository.class);
    transactionsSummaryRepository = mock(TransactionsSummaryRepository.class);
    transactionRepository = mock(TransactionRepository.class);
    invoiceRepositoryMock = mock(InvoiceJpaRepository.class);
    transactionService = new TransactionService(
        transactionRepository,
        accountHolderJpaRepository,
        transactionsSummaryRepository,
        invoiceRepositoryMock
    );

    when(transactionRepository.findByAccountIdAndStatusBetweenInstants(any(), any(), any(), any()))
        .thenReturn(transactions());
  }

  @Test
  void refresh_summaries_with_last_month_summary() {
    YearMonth lastMonth = YearMonth.now().minusMonths(1);
    Instant now = Instant.now();
    when(transactionsSummaryRepository.getByAccountIdAndYearMonth(
        JOE_DOE_ACCOUNT_ID,
        lastMonth.getYear(),
        lastMonth.getMonthValue() - 1
    )).thenReturn(lastMonthlySummary(now));
    ArgumentCaptor<String> accountCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> yearCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<MonthlyTransactionsSummary> summaryCaptor =
        ArgumentCaptor.forClass(MonthlyTransactionsSummary.class);

    transactionService.refreshMonthSummary(JOE_DOE_ACCOUNT_ID, new Fraction(), YearMonth.now(),
        transactions());
    verify(transactionsSummaryRepository).updateYearMonthSummary(
        accountCaptor.capture(),
        yearCaptor.capture(),
        summaryCaptor.capture()
    );
    assertEquals(JOE_DOE_ACCOUNT_ID, accountCaptor.getValue());
    assertEquals(YearMonth.now().getYear(), yearCaptor.getValue());
    assertEquals(refreshedSummary1(summaryCaptor.getValue().getUpdatedAt()),
        summaryCaptor.getValue());
  }

  @Test
  void refresh_summaries_without_last_month_summary() {
    YearMonth lastMonth = YearMonth.now().minusMonths(1);
    when(transactionsSummaryRepository.getByAccountIdAndYearMonth(
        JOE_DOE_ACCOUNT_ID,
        lastMonth.getYear(),
        lastMonth.getMonthValue() - 1
    )).thenReturn(null);
    ArgumentCaptor<String> accountCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> yearCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<MonthlyTransactionsSummary> summaryCaptor =
        ArgumentCaptor.forClass(MonthlyTransactionsSummary.class);

    transactionService.refreshMonthSummary(JOE_DOE_ACCOUNT_ID, new Fraction(), YearMonth.now(),
        transactions());
    verify(transactionsSummaryRepository).updateYearMonthSummary(
        accountCaptor.capture(),
        yearCaptor.capture(),
        summaryCaptor.capture()
    );
    assertEquals(JOE_DOE_ACCOUNT_ID, accountCaptor.getValue());
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
        .cashFlow(new Fraction(BigInteger.TWO))
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
