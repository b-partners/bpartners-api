package app.bpartners.api.repository.jpa.model;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "\"transaction_category\"")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@EqualsAndHashCode
public class HTransactionCategory {
  public static final String ID_TRANSACTION_ATTRIBUTE = "idTransaction";
  public static final String ID_ACCOUNT_ATTRIBUTE = "idAccount";
  public static final String ID_CATEGORY_TMPL_ATTRIBUTE = "idCategoryTemplate";
  public static final String TYPE_ATTRIBUTE = "type";
  public static final String VAT_ATTRIBUTE = "vat";
  public static final String CREATED_DATETIME_ATTRIBUTE = "createdDatetime";
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;
  private String idTransaction;
  private String idAccount;
  @Column(name = "id_transaction_category_tmpl")
  private String idCategoryTemplate;
  @Column(name = "\"type\"")
  private String type;
  private String vat;
  @CreationTimestamp
  private Instant createdDatetime;
  private String comment;
  @Transient
  private String description;

  public HTransactionCategory(String id, String idTransaction, String idAccount,
                              String idCategoryTemplate, String type, String vat,
                              Instant createdDatetime, String comment, String description) {
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
