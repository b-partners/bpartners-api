package app.bpartners.api.repository;

import app.bpartners.api.model.TransactionsSummary;

public interface TransactionsSummaryRepository {
  TransactionsSummary getByYear(int year);
}
