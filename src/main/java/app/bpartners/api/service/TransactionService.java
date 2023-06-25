package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.JustifyTransaction;
import app.bpartners.api.model.MonthlyTransactionsSummary;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionsSummary;
import app.bpartners.api.repository.TransactionRepository;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apfloat.Aprational;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import static app.bpartners.api.endpoint.rest.model.TransactionStatus.BOOKED;
import static org.springframework.scheduling.config.ScheduledTaskRegistrar.CRON_DISABLED;

@Service
@AllArgsConstructor
@Slf4j
public class TransactionService {
  private final TransactionRepository repository;
  private final TransactionsSummaryRepository summaryRepository;
  private final AccountService accountService;

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

  public List<Transaction> getPersistedByIdAccount(String idAccount, PageFromOne page,
                                                   BoundedPageSize pageSize) {
    int pageValue = page == null ? 0 : page.getValue() - 1;
    int pageSizeValue = pageSize == null ? 30 : pageSize.getValue();
    return repository.findPersistedByIdAccount(idAccount, pageValue, pageSizeValue);
  }

  public Transaction getById(String transactionId) {
    return repository.findById(transactionId);
  }

  public TransactionsSummary getTransactionsSummary(String idUser, Integer year) {
    if (year == null) {
      year = LocalDate.now().getYear();
    }
    return summaryRepository.getByIdUserAndYear(idUser, year);
  }

  public Transaction justifyTransaction(String idTransaction, String idInvoice) {
    return repository.save(JustifyTransaction.builder()
        .idTransaction(idTransaction)
        .idInvoice(idInvoice)
        .build());
  }

  public void refreshCurrentYearSummary(Account account) {
    int actualYear = Year.now().getValue();
    List<Transaction> yearlyTransactions =
        repository.findByAccountIdAndStatusBetweenInstants(
            account.getId(), BOOKED, getFirstDayOfYear(actualYear), getLastDayOfYear(actualYear));
    for (int i = Month.JANUARY.getValue(); i <= Month.DECEMBER.getValue(); i++) {
      YearMonth yearMonth = YearMonth.of(actualYear, i);
      List<Transaction> monthlyTransactions = filterByTwoInstants(yearlyTransactions,
          getFirstDayOfMonth(yearMonth),
          getLastDayOfMonth(yearMonth));
      refreshMonthSummary(account, YearMonth.of(actualYear, i), monthlyTransactions);
    }
  }

  public void refreshMonthSummary(
      Account account, YearMonth yearMonth, List<Transaction> transactions) {
    AtomicReference<Fraction> incomeReference = new AtomicReference<>(new Fraction());
    AtomicReference<Fraction> outcomeReference = new AtomicReference<>(new Fraction());
    transactions.forEach(
        transaction -> {
          if (transaction.getType().equals(TransactionTypeEnum.INCOME)) {
            incomeReference.set(
                incomeReference.get().operate(transaction.getAmount(), Aprational::add));
          }
          if (transaction.getType().equals(TransactionTypeEnum.OUTCOME)) {
            outcomeReference.set(
                outcomeReference.get().operate(transaction.getAmount(), Aprational::add));
          }
        });

    Fraction incomeValue = incomeReference.get();
    Fraction outcomeValue = outcomeReference.get();
    MonthlyTransactionsSummary actualSummary =
        getByIdUserAndYearMonth(account.getUserId(), yearMonth);

    saveSummariesByYearMonth(
        account.getUserId(),
        yearMonth.getYear(),
        MonthlyTransactionsSummary
            .builder()
            .id(actualSummary == null ? null : actualSummary.getId())
            .income(incomeValue)
            .outcome(outcomeValue)
            .cashFlow(account.getAvailableBalance())
            .month(yearMonth.getMonthValue() - 1)
            .build());
  }

  public void saveSummariesByYearMonth(
      String idUser, Integer year, MonthlyTransactionsSummary monthlyTransactionsSummary) {
    summaryRepository.updateYearMonthSummary(idUser, year, monthlyTransactionsSummary);
  }

  public MonthlyTransactionsSummary getByIdUserAndYearMonth(
      String idUser, YearMonth yearMonth) {
    return summaryRepository.getByIdUserAndYearMonth(
        idUser, yearMonth.getYear(), yearMonth.getMonthValue() - 1);
  }

  //TODO: check if 1 hour of refresh is enough or too much
  //TODO: note that account (balance) is _NOT_ updated by this scheduled task anymore
  @Scheduled(cron = CRON_DISABLED, zone = "Europe/Paris")
  public void refreshTransactionsSummaries() {
    List<Account> activeAccounts = accountService.findAllActiveAccounts();
    activeAccounts.forEach(
        account -> {
          refreshCurrentYearSummary(account);
          log.info("Transactions summaries refreshed for {}", account.describeInfos());
        }
    );
  }
}