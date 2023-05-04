package app.bpartners.api.repository;

import app.bpartners.api.model.MonthlyTransactionsSummary;
import app.bpartners.api.model.TransactionsSummary;

public interface TransactionsSummaryRepository {
  TransactionsSummary getByAccountIdAndYear(String accountId, int year);

  TransactionsSummary getByAccountHolderIdAndYear(String accountHolderId, int year);

  MonthlyTransactionsSummary updateYearMonthSummary(
      String accountId, int year,
      MonthlyTransactionsSummary monthlyTransactionsSummary);

  MonthlyTransactionsSummary getByAccountIdAndYearMonth(String accountId, int year, int month);

  void removeAll(String userId);
}
