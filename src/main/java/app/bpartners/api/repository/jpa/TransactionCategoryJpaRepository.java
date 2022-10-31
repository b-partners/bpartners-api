package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HTransactionCategory;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionCategoryJpaRepository
    extends JpaRepository<HTransactionCategory, String> {
  List<HTransactionCategory> findAllByIdAccount(String idAccount);

  /*
   * TODO: use JPQL instead of native SQL
   * counts by type and idAccount
   * if type is null,
   * it checks the id_transaction_category_tmpl.
   */
  @Query(value =
      "select count(tc.*) from \"transaction_category\" tc "
          + "where tc.id_account = ?1 "
          + "and (tc.type = ?2 or tc.id_transaction_category_tmpl = ?2) "
          + "and tc.created_datetime between ?3 and ?4 ",
      nativeQuery = true)
  Long countByCriteria(String idAccount, String typeOrIdTmpl, LocalDateTime from, LocalDateTime to);
}