package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HTransactionCategory;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionCategoryJpaRepository
    extends JpaRepository<HTransactionCategory, String> {
  @Query(value = "select "
      + "new HTransactionCategory(coalesce(tc.id, 'nullValue'),"
      + "tc.idTransaction,"
      + "coalesce(tc.idAccount, ?1),"
      + "coalesce(tc.idCategoryTemplate, template.id),"
      + "coalesce(tc.type, template.type),"
      + "tc.createdDatetime,"
      + "tc.comment,"
      + "tc.description )"
      + "from HTransactionCategoryTemplate template "
      + "left join HTransactionCategory tc on "
      + "(template.id = tc.idCategoryTemplate) "
      + "where tc.idAccount = ?1 "
      + "or tc.idAccount is null")
  List<HTransactionCategory> findAllByIdAccount(String idAccount);


  HTransactionCategory findTopByIdTransactionOrderByCreatedDatetimeDesc(String idTransaction);

  @Query(value = "select "
      + " count(template.type)"
      + "from HTransactionCategoryTemplate template "
      + "left join HTransactionCategory tc on "
      + "(template.id = tc.idCategoryTemplate) "
      + " where (tc.idAccount = ?1 or tc.idAccount is null)"
      + "and (coalesce(tc.type, template.type) = ?2)"
      + "and (tc.comment is null or tc.comment = ?3)"
      + "and (CAST(coalesce(tc.createdDatetime, '2022-01-01 00:00:00') as timestamp) "
      + "between CAST(?4 as timestamp ) and CAST(?5 as timestamp ))"
  )
  long countByCriteria(String idAccount, String type, String comment, Instant from,
                       Instant to);
}