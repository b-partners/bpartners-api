package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.MonthlyTransactionsSummary;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionInvoice;
import app.bpartners.api.model.TransactionsSummary;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.TransactionRepository;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apfloat.Aprational;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static app.bpartners.api.endpoint.rest.model.TransactionStatus.BOOKED;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static org.springframework.transaction.annotation.Isolation.SERIALIZABLE;

@Service
@AllArgsConstructor
@Slf4j
public class TransactionService {
  private final TransactionRepository repository;
  private final AccountHolderJpaRepository holderJpaRepository;
  private final TransactionsSummaryRepository summaryRepository;
  private final InvoiceJpaRepository invoiceRepository;

  private static Instant getFirstDayOfYear(int year) {
    return getFirstDayOfMonth(YearMonth.of(year, Month.JANUARY.getValue()));

  }

  private static Instant getLastDayOfYear(int year) {
    return getLastDayOfMonth(YearMonth.of(year, Month.DECEMBER.getValue()));
  }

  private static Instant getFirstDayOfMonth(YearMonth yearMonth) {
    return yearMonth
        .atDay(1)
        .atStartOfDay()
        .toInstant(ZoneOffset.UTC)
        .truncatedTo(ChronoUnit.DAYS);
  }

  private static Instant getLastDayOfMonth(YearMonth yearMonth) {
    return yearMonth
        .atEndOfMonth()
        .atStartOfDay()
        .plusDays(1)
        .toInstant(ZoneOffset.UTC)
        .truncatedTo(ChronoUnit.DAYS)
        .minusSeconds(1);
  }

  private static List<Transaction> filterByTwoInstants(
      List<Transaction> transactions, Instant from, Instant to) {
    return transactions.stream().filter(
            transaction -> transaction.getPaymentDatetime().isAfter(from)
                &&
                transaction.getPaymentDatetime().isBefore(to)
        )
        .collect(Collectors.toUnmodifiableList());
  }

  public List<Transaction> getTransactionsByAccountId(String accountId) {
    return repository.findByAccountId(accountId);
  }

  public TransactionsSummary getTransactionsSummary(String accountId, Integer year) {
    if (year == null) {
      year = LocalDate.now().getYear();
    }
    return summaryRepository.getByAccountIdAndYear(accountId, year);
  }

  //TODO: refactor invoice -> transactionInvoice to appropriate mapper
  public Transaction justifyTransaction(String idTransaction, String idInvoice) {
    Transaction transaction = repository.getById(idTransaction);
    HInvoice invoice = invoiceRepository.findById(idInvoice).orElseThrow(
        () -> new NotFoundException(
            "Invoice." + idInvoice + " is not found")
    );
    log.info("Invoice=" + invoice.getId() + ", " + invoice.getFileId());
    return repository.save(transaction.toBuilder()
        .transactionInvoice(TransactionInvoice.builder()
            .invoiceId(invoice.getId())
            .fileId(invoice.getFileId())
            .build())
        .build());
  }

  @Transactional(isolation = SERIALIZABLE)
  public void refreshCurrentYearSummary(
      String accountId,
      Fraction cashFlow) {
    int actualYear = Year.now().getValue();
    List<Transaction> yearlyTransactions =
        repository.findByAccountIdAndStatusBetweenInstants(accountId, BOOKED,
            getFirstDayOfYear(actualYear), getLastDayOfYear(actualYear));
    for (int i = Month.JANUARY.getValue(); i <= Month.DECEMBER.getValue(); i++) {
      YearMonth yearMonth = YearMonth.of(actualYear, i);
      List<Transaction> monthlyTransactions = filterByTwoInstants(yearlyTransactions,
          getFirstDayOfMonth(yearMonth),
          getLastDayOfMonth(yearMonth));
      refreshMonthSummary(accountId, cashFlow, YearMonth.of(actualYear, i), monthlyTransactions);
    }
  }

  public void refreshMonthSummary(
      String accountId, Fraction cashFlow, YearMonth yearMonth,
      List<Transaction> transactions) {
    Fraction[] summaryParameters = new Fraction[] {new Fraction(), new Fraction()};
    transactions.forEach(
        transaction -> {
          if (transaction.getType().equals(TransactionTypeEnum.INCOME)) {
            summaryParameters[0] =
                summaryParameters[0].operate(transaction.getAmount(), Aprational::add);
          }
          if (transaction.getType().equals(TransactionTypeEnum.OUTCOME)) {
            summaryParameters[1] =
                summaryParameters[1].operate(transaction.getAmount(), Aprational::add);
          }
        });
    MonthlyTransactionsSummary previousSummary =
        getByAccountIdAndYearMonth(accountId, yearMonth.minusMonths(1));
    MonthlyTransactionsSummary actualSummary =
        getByAccountIdAndYearMonth(accountId, yearMonth);
    Fraction actualBalance =
        summaryParameters[0].operate(summaryParameters[1], Aprational::subtract);
    Fraction previousCashFlow = previousSummary == null ? cashFlow :
        previousSummary.getCashFlow();
    String idMonthlyTransactionSummary = actualSummary == null ? null : actualSummary.getId();
    saveSummariesByYearMonth(
        accountId,
        yearMonth.getYear(),
        MonthlyTransactionsSummary
            .builder()
            .id(idMonthlyTransactionSummary)
            .income(summaryParameters[0])
            .outcome(summaryParameters[1])
            .cashFlow(previousCashFlow.operate(actualBalance, Aprational::add))
            .month(yearMonth.getMonthValue() - 1)
            .build());
  }

  public void saveSummariesByYearMonth(
      String accountId, Integer year,
      MonthlyTransactionsSummary monthlyTransactionsSummary) {
    summaryRepository.updateYearMonthSummary(accountId, year, monthlyTransactionsSummary);
  }

  public MonthlyTransactionsSummary getByAccountIdAndYearMonth(
      String accountId, YearMonth yearMonth) {
    return summaryRepository.getByAccountIdAndYearMonth(accountId, yearMonth.getYear(),
        yearMonth.getMonthValue() - 1);
  }

  @Scheduled(fixedDelay = 60 * 1_000)
  public void refreshTransactionsSummaries() {
    holderJpaRepository.findAllGroupByAccountId().forEach(
        accountHolder -> {
          refreshCurrentYearSummary(
              accountHolder.getAccountId(), parseFraction(accountHolder.getInitialCashflow()));
          log.info("Transactions summaries refreshed for {}", accountHolder);
        }
    );
  }
}