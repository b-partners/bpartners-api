package app.bpartners.api.repository.jpa.model;

import static jakarta.persistence.GenerationType.IDENTITY;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "\"transaction_category\"")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@EqualsAndHashCode
public class HTransactionCategory {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  public static final String ID_TRANSACTION_ATTRIBUTE = "idTransaction";
  private String idTransaction;
  private String idAccount;
  public static final String ID_ACCOUNT_ATTRIBUTE = "idAccount";

  @Column(name = "id_transaction_category_tmpl")
  private String idCategoryTemplate;

  public static final String ID_CATEGORY_TMPL_ATTRIBUTE = "idCategoryTemplate";

  @Column(name = "\"type\"")
  private String type;

  public static final String TYPE_ATTRIBUTE = "type";

  private String vat;
  public static final String VAT_ATTRIBUTE = "vat";
  @CreationTimestamp private Instant createdDatetime;
  public static final String CREATED_DATETIME_ATTRIBUTE = "createdDatetime";
  private String comment;
  @Transient private String description;

  public HTransactionCategory(
      String id,
      String idTransaction,
      String idAccount,
      String idCategoryTemplate,
      String type,
      String vat,
      Instant createdDatetime,
      String comment,
      String description) {
    this.id = id;
    this.idTransaction = idTransaction;
    this.idAccount = idAccount;
    this.idCategoryTemplate = idCategoryTemplate;
    this.type = type;
    this.vat = vat;
    this.createdDatetime = createdDatetime;
    this.comment = comment;
    this.description = description;
  }
}
