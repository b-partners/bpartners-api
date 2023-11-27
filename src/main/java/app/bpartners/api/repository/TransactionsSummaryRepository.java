package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.model.MonthlyTransactionsSummary;
import app.bpartners.api.model.TransactionsSummary;
import java.util.List;

public interface TransactionsSummaryRepository {
  List<TransactionsSummary> getByIdUser(String idUser);

  List<TransactionsSummary> saveAll(List<TransactionsSummary> toSave);

  TransactionsSummary getByIdUserAndYearAndStatus(String idUser, int year, EnableStatus status);

  TransactionsSummary getEnabledByAccountHolderIdAndYear(String accountHolderId, int year);

  MonthlyTransactionsSummary updateYearMonthSummary(
      String idUser, int year, MonthlyTransactionsSummary monthlySummary);

  MonthlyTransactionsSummary getEnabledByIdUserAndYearMonth(String idUser, int year, int month);

  void removeAll(String userId);
}
