package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HMonthlyTransactionsSummary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionsSummaryJpaRepository
    extends JpaRepository<HMonthlyTransactionsSummary, String> {
  List<HMonthlyTransactionsSummary> getByIdUser(String accountId);

  List<HMonthlyTransactionsSummary> getByIdAccountAndYear(String accountId, int year);

  @Query("select m from HMonthlyTransactionsSummary m, HAccount a, HAccountHolder ah "
      + " where m.idAccount = a.id and a.user.id = ah.idUser and ah.id = ?1 and m.year = ?2 ")
  List<HMonthlyTransactionsSummary> getByIdAccountHolderIdAndYear(String accountHolderId, int year);

  HMonthlyTransactionsSummary getByIdAccountAndYearAndMonth(String accountId, int year, int month);

  Optional<HMonthlyTransactionsSummary> findByIdAccountAndYearAndMonth(
      String accountId, int year, int month);
}
