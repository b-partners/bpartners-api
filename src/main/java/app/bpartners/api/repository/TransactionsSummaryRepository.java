package app.bpartners.api.repository;

import app.bpartners.api.model.MonthlyTransactionsSummary;
import app.bpartners.api.model.TransactionsSummary;

public interface TransactionsSummaryRepository {
  TransactionsSummary getByIdUserAndYear(String idUser, int year);

  TransactionsSummary getByAccountHolderIdAndYear(String accountHolderId, int year);

  MonthlyTransactionsSummary updateYearMonthSummary(
      String idUser, int year, MonthlyTransactionsSummary monthlySummary);

  MonthlyTransactionsSummary getByIdUserAndYearMonth(String idUser, int year, int month);

  void removeAll(String userId);
}
