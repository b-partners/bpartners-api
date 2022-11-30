package app.bpartners.api.repository.jpa;

import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
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
      + "coalesce(tc.vat, template.vat),"
      + "tc.createdDatetime,"
      + "tc.comment,"
      + "template.description )"
      + "from HTransactionCategoryTemplate template "
      + "left join HTransactionCategory tc on "
      + "(template.id = tc.idCategoryTemplate) "
      + "where (tc.idAccount = ?1 "
      + "or tc.idAccount is null) ")
  List<HTransactionCategory> findAllByIdAccount(String idAccount);

  @Query(value = "select "
      + "new HTransactionCategory(coalesce(tc.id, 'nullValue'),"
      + "tc.idTransaction,"
      + "coalesce(tc.idAccount, ?1),"
      + "coalesce(tc.idCategoryTemplate, template.id),"
      + "coalesce(tc.type, template.type),"
      + "coalesce(tc.vat, template.vat),"
      + "tc.createdDatetime,"
      + "tc.comment,"
      + "template.description )"
      + "from HTransactionCategoryTemplate template "
      + "left join HTransactionCategory tc on "
      + "(template.id = tc.idCategoryTemplate) "
      + "where (tc.idAccount = ?1 "
      + "or tc.idAccount is null) "
      + "and template.transactionType = ?2")
  List<HTransactionCategory> findAllByIdAccountAndType(String idAccount, TransactionTypeEnum type);

  HTransactionCategory findTopByIdTransactionOrderByCreatedDatetimeDesc(String idTransaction);

  @Query(value = "select "
      + " count(template.type)"
      + "from HTransactionCategoryTemplate template "
      + "left join HTransactionCategory tc on "
      + "(template.id = tc.idCategoryTemplate) "
      + " where (tc.idAccount = ?1 or tc.idAccount is null)"
      + "and (coalesce(tc.type, template.type) = ?2)"
      + "and (CAST(coalesce(tc.createdDatetime, '2021-01-01 00:00:00') as timestamp) "
      + "between CAST(?3 as timestamp ) and CAST(?4 as timestamp ))"
  )
  long countByCriteria(String idAccount, String type, Instant from,
                       Instant to);
}