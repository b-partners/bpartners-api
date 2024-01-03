package app.bpartners.api.unit.service;

import static app.bpartners.api.integration.conf.utils.TestUtils.CREDIT_SIDE;
import static app.bpartners.api.integration.conf.utils.TestUtils.DEBIT_SIDE;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.model.Money.fromMajor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Money;
import app.bpartners.api.model.MonthlyTransactionsSummary;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.repository.BridgeTransactionRepository;
import app.bpartners.api.repository.DbTransactionRepository;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import app.bpartners.api.service.AccountService;
import app.bpartners.api.service.FileService;
import app.bpartners.api.service.InvoiceService;
import app.bpartners.api.service.TransactionService;
import app.bpartners.api.service.UserService;
import app.bpartners.api.service.aws.S3Service;
import java.math.BigInteger;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TransactionServiceSummariesTest {
  TransactionService transactionService;
  DbTransactionRepository dbTransactionRepository;
  BridgeTransactionRepository bridgeTransactionRepository;
  TransactionsSummaryRepository transactionsSummaryRepository;
  AccountService accountService;
  InvoiceService invoiceServiceMock;
  S3Service s3ServiceMock;
  UserService userServiceMock;
  FileService fileServiceMock;

  private static Account joeDoeAccount() {
    return Account.builder().userId(JOE_DOE_ID).availableBalance(new Money()).build();
  }

  @BeforeEach
  void setUp() {
    transactionsSummaryRepository = mock(TransactionsSummaryRepository.class);
    dbTransactionRepository = mock(DbTransactionRepository.class);
    bridgeTransactionRepository = mock(BridgeTransactionRepository.class);
    accountService = mock(AccountService.class);
    invoiceServiceMock = mock(InvoiceService.class);
    s3ServiceMock = mock(S3Service.class);
    userServiceMock = mock(UserService.class);
    fileServiceMock = mock(FileService.class);
    transactionService =
        new TransactionService(
            dbTransactionRepository,
            bridgeTransactionRepository,
            transactionsSummaryRepository,
            accountService,
            invoiceServiceMock,
            s3ServiceMock,
            userServiceMock,
            fileServiceMock);

    when(dbTransactionRepository.findByAccountIdAndStatusBetweenInstants(
            any(), any(), any(), any()))
        .thenReturn(transactions());
  }

  @Test
  void refresh_summaries_with_last_month_summary() {
    YearMonth lastMonth = YearMonth.now().minusMonths(1);
    Instant now = Instant.now();
    when(transactionsSummaryRepository.getByIdUserAndYearMonth(
            JOE_DOE_ID, lastMonth.getYear(), lastMonth.getMonthValue() - 1))
        .thenReturn(lastMonthlySummary(now));
    ArgumentCaptor<String> idUserCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> yearCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<MonthlyTransactionsSummary> summaryCaptor =
        ArgumentCaptor.forClass(MonthlyTransactionsSummary.class);

    transactionService.refreshMonthSummary(joeDoeAccount(), YearMonth.now(), transactions());
    verify(transactionsSummaryRepository)
        .updateYearMonthSummary(
            idUserCaptor.capture(), yearCaptor.capture(), summaryCaptor.capture());
    assertEquals(JOE_DOE_ID, idUserCaptor.getValue());
    assertEquals(YearMonth.now().getYear(), yearCaptor.getValue());
    assertEquals(
        refreshedSummary1(summaryCaptor.getValue().getUpdatedAt()), summaryCaptor.getValue());
  }

  @Test
  void refresh_summaries_without_last_month_summary() {
    YearMonth lastMonth = YearMonth.now().minusMonths(1);
    when(transactionsSummaryRepository.getByIdUserAndYearMonth(
            JOE_DOE_ID, lastMonth.getYear(), lastMonth.getMonthValue() - 1))
        .thenReturn(null);
    ArgumentCaptor<String> idUserCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> yearCaptor = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<MonthlyTransactionsSummary> summaryCaptor =
        ArgumentCaptor.forClass(MonthlyTransactionsSummary.class);

    transactionService.refreshMonthSummary(joeDoeAccount(), YearMonth.now(), transactions());
    verify(transactionsSummaryRepository)
        .updateYearMonthSummary(
            idUserCaptor.capture(), yearCaptor.capture(), summaryCaptor.capture());
    assertEquals(JOE_DOE_ID, idUserCaptor.getValue());
    assertEquals(YearMonth.now().getYear(), yearCaptor.getValue());
    assertEquals(
        refreshedSummary2(summaryCaptor.getValue().getUpdatedAt()), summaryCaptor.getValue());
  }

  private Transaction.TransactionBuilder transactionWith100Value() {
    return Transaction.builder().amount(fromMajor(10000));
  }

  private MonthlyTransactionsSummary lastMonthlySummary(Instant updatedAt) {
    return MonthlyTransactionsSummary.builder()
        .cashFlow(new Fraction(BigInteger.TWO))
        .updatedAt(updatedAt)
        .build();
  }

  private MonthlyTransactionsSummary refreshedSummary1(Instant updatedAt) {
    return MonthlyTransactionsSummary.builder()
        .income(new Fraction(BigInteger.valueOf(10000)))
        .month(YearMonth.now().getMonthValue() - 1)
        .outcome(new Fraction(BigInteger.valueOf(10000)))
        .cashFlow(new Fraction())
        .updatedAt(updatedAt)
        .build();
  }

  private MonthlyTransactionsSummary refreshedSummary2(Instant updatedAt) {
    return MonthlyTransactionsSummary.builder()
        .month(YearMonth.now().getMonthValue() - 1)
        .income(new Fraction(BigInteger.valueOf(10000)))
        .outcome(new Fraction(BigInteger.valueOf(10000)))
        .cashFlow(new Fraction())
        .updatedAt(updatedAt)
        .build();
  }

  private List<Transaction> transactions() {
    Instant now = Instant.now();
    return List.of(
        transactionWith100Value().side(CREDIT_SIDE).paymentDatetime(now).build(),
        transactionWith100Value().side(DEBIT_SIDE).paymentDatetime(now).build());
  }
}
