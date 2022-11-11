package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HMonthlyTransactionsSummary;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionsSummaryJpaRepository
    extends JpaRepository<HMonthlyTransactionsSummary, String> {
  List<HMonthlyTransactionsSummary> getByYear(int year);
}
