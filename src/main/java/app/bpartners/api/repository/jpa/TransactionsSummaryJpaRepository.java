package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HMonthlyTransactionsSummary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionsSummaryJpaRepository
    extends JpaRepository<HMonthlyTransactionsSummary, String> {
  List<HMonthlyTransactionsSummary> getByIdAccountAndYear(String accountId, int year);

  HMonthlyTransactionsSummary getByIdAccountAndYearAndMonth(String accountId, int year, int month);

  Optional<HMonthlyTransactionsSummary> findByIdAccountAndYearAndMonth(
      String accountId, int year, int month);
}
