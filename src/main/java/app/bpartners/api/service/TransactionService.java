package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.MonthlyTransactionsSummary;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionsSummary;
import app.bpartners.api.repository.TransactionRepository;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apfloat.Aprational;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import static app.bpartners.api.endpoint.rest.model.TransactionStatus.BOOKED;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Service
@Slf4j
@AllArgsConstructor
public class TransactionService {
  private final TransactionRepository repository;
  private final AccountHolderJpaRepository holderJpaRepository;
  private final TransactionsSummaryRepository summaryRepository;

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

  public List<Transaction> getTransactionsByAccountId(String accountId) {
    return repository.findByAccountId(accountId);
  }

  public TransactionsSummary getTransactionsSummary(String accountId, Integer year) {
    if (year == null) {
      year = LocalDate.now().getYear();
    }
    return summaryRepository.getByAccountIdAndYear(accountId, year);
  }

  public MonthlyTransactionsSummary refreshCurrentMonthSummary(
      String accountId,
      Fraction cashFlow) {
    YearMonth yearMonth = YearMonth.now();
    List<Transaction> transactions = repository.findByAccountIdAndStatusBetweenInstants(
        accountId, BOOKED, getFirstDayOfMonth(yearMonth), getLastDayOfMonth(yearMonth));
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
    Fraction actualBalance =
        summaryParameters[0].operate(summaryParameters[1], Aprational::subtract);
    return saveSummariesByYearMonth(
        accountId,
        yearMonth.getYear(),
        MonthlyTransactionsSummary
            .builder()
            .income(summaryParameters[0])
            .outcome(summaryParameters[1])
            .cashFlow(
                getPreviousCashFlow(accountId, cashFlow, yearMonth)
                    .operate(actualBalance, Aprational::add))
            .month(yearMonth.getMonthValue())
            .build());
  }

  private Fraction getPreviousCashFlow(
      String accountId, Fraction initialCashFlow, YearMonth yearMonth) {
    MonthlyTransactionsSummary monthlySummary =
        getByAccountIdAndYearMonth(accountId, yearMonth.minusMonths(1));
    return monthlySummary == null ? initialCashFlow : monthlySummary.getCashFlow();
  }

  public MonthlyTransactionsSummary saveSummariesByYearMonth(
      String accountId, Integer year,
      MonthlyTransactionsSummary monthlyTransactionsSummary) {
    return summaryRepository.updateYearMonthSummary(accountId, year, monthlyTransactionsSummary);
  }

  public MonthlyTransactionsSummary getByAccountIdAndYearMonth(
      String accountId, YearMonth yearMonth) {
    return summaryRepository.getByAccountIdAndYearMonth(accountId, yearMonth.getYear(),
        yearMonth.getMonthValue());
  }

  @Scheduled(cron = "0 0 * * * ?")
  public void refreshTransactionsSummaries() {
    holderJpaRepository.findAllGroupByAccountId().forEach(
        accountHolder -> refreshCurrentMonthSummary(
            accountHolder.getAccountId(), parseFraction(accountHolder.getInitialCashflow()))
    );
  }
}