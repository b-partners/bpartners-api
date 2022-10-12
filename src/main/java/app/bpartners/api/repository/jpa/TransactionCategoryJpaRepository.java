package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HTransactionCategory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionCategoryJpaRepository
    extends JpaRepository<HTransactionCategory, String> {
  @Query(value = "select tc.* from \"transaction_category\" tc where tc.id_transaction = ?1 order "
      + "by tc.created_datetime desc limit 1",
      nativeQuery = true)
  Optional<HTransactionCategory> findFirstByCreatedDatetimeAndIdTransaction(String idTransaction);

  @Query(value = "select tc.* from \"transaction_category\" tc where tc.id_account = ?1 "
      + "and tc.type is null and tc.vat is null and tc.id_transaction_category_tmpl = "
      + "?2 order by tc.created_datetime desc limit 1", nativeQuery = true)
  HTransactionCategory findByCriteriaOrderByCreatedDatetime(
      String idAccount, String idCategoryTmpl);

  @Query(value = "select tc.* from \"transaction_category\" tc where tc.id_account = ?1 "
      + "and tc.type = ?2 and tc.vat = ?3 and tc.id_transaction_category_tmpl is null"
      + " order by tc.created_datetime desc limit 1", nativeQuery = true)
  HTransactionCategory findByCriteriaOrderByCreatedDatetime(
      String idAccount, String type, Integer vat);

  List<HTransactionCategory> findAllByIdAccount(String idAccount);

  @Query(value =
      "select count(tc.*) from \"transaction_category\" tc "
          + "where tc.id_account = ?1 "
          + "and tc.type = ?2 "
          + "and tc.created_datetime between ?3 and ?4 ",
      nativeQuery = true)
  Long countByCriteria(String idAccount, String type, LocalDateTime from, LocalDateTime to);
}