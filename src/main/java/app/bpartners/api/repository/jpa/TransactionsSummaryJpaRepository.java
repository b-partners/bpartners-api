package app.bpartners.api.repository.jpa;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.repository.jpa.model.HMonthlyTransactionsSummary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionsSummaryJpaRepository
    extends JpaRepository<HMonthlyTransactionsSummary, String> {
  List<HMonthlyTransactionsSummary> getByIdUser(String idUser);

  List<HMonthlyTransactionsSummary> getByIdUserAndYearAndTransactionSummaryStatus(String idUser,
                                                                                  int year,
                                                                                  EnableStatus enableStatus);

  @Query("select m from HMonthlyTransactionsSummary m, HUser u, HAccountHolder ah "
      + " where m.idUser = u.id and u.id = ah.idUser and ah.id = ?1"
      + " and m.year = ?2 and m.transactionSummaryStatus = ?3")
  List<HMonthlyTransactionsSummary> getByIdAccountHolderIdAndYearAndStatus(String accountHolderId,
                                                                           int year,
                                                                           String enableStatus);

  HMonthlyTransactionsSummary getByIdUserAndYearAndMonthAndTransactionSummaryStatus(String idUser,
                                                                                    int year,
                                                                                    int month,
                                                                                    EnableStatus enableStatus);

  Optional<HMonthlyTransactionsSummary> findByIdUserAndYearAndMonth(
      String idUser, int year, int month);
}
