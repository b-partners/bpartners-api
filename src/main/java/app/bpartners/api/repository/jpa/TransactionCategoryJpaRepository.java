package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HTransactionCategory;
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

  List<HTransactionCategory> findAllByIdAccountAndUserDefined(
      String idAccount,
      boolean userDefined);
}
