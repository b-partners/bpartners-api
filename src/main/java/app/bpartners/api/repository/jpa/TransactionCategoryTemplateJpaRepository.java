package app.bpartners.api.repository.jpa;

import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import app.bpartners.api.repository.jpa.model.HTransactionCategoryTemplate;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionCategoryTemplateJpaRepository
    extends JpaRepository<HTransactionCategoryTemplate, String> {
  List<HTransactionCategoryTemplate> findAllByType(String type);

  HTransactionCategoryTemplate findByTypeAndTransactionType(
      String type,
      TransactionTypeEnum transactionType);

  @Query(value =
      "select template.*, case when count is null then 0 else count end"
          + " from transaction_category_template template"
          + "         left join (select id_category_tmpl,"
          + " count(id_category_tmpl) as count"
          + "                    from (select template.id as id,"
          + "       tc.id_transaction,"
          + "       tc.id_account,"
          + "       template.id as id_category_tmpl,"
          + "       tc.comment  as comment"
          + " from transaction_category_template template"
          + "         join transaction_category tc on"
          + "    (template.id = tc.id_transaction_category_tmpl)"
          + " where (tc.id_account = ?1"
          + "    or tc.id_account is null)"
          + " and tc.created_datetime between ?2 and ?3) as template_view"
          +
          "                    group by (template_view.id_category_tmpl, comment)) as template_count"
          + "                   on template.id = template_count.id_category_tmpl",
      nativeQuery = true)
  List<HTransactionCategoryTemplate> findAllByIdAccount(
      String idAccount, LocalDate begin, LocalDate end);

  //TODO: try to reproduce this but using ORM
  @Query(value =
      "select template.*, case when count is null then 0 else count end"
          + " from (select tct.* from transaction_category_template tct"
          + " where tct.transaction_type = cast(?1 as transaction_type)) as template"
          + " left join (select id_category_tmpl,"
          + " count(id_category_tmpl) as count"
          + " from (select template.id as id,"
          + " tc.id_transaction,"
          + " tc.id_account,"
          + " template.id as id_category_tmpl,"
          + " tc.comment  as comment"
          + " from transaction_category_template as template"
          + "   join transaction_category tc on"
          + "    (template.id = tc.id_transaction_category_tmpl)"
          + "    where (tc.id_account = ?2 or tc.id_account is null)"
          + "   and tc.created_datetime between ?3 and ?4) as template_view"
          + " group by (template_view.id_category_tmpl, comment)) as template_count"
          + " on template.id = template_count.id_category_tmpl",
      nativeQuery = true)
  List<HTransactionCategoryTemplate> findAllByIdAccountAndType(
      String type, String idAccount, LocalDate begin, LocalDate end);
}
