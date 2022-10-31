package app.bpartners.api.repository.jpa.model;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
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
  @CreationTimestamp
  private Instant createdDatetime;
  public static final String CREATED_DATETIME_ATTRIBUTE = "createdDatetime";

  public HTransactionCategory(
      String idAccount, String idCategoryTemplate, String type, String vat) {
    this.idAccount = idAccount;
    this.idCategoryTemplate = idCategoryTemplate;
    this.type = type;
    this.vat = vat;
  }

  public boolean isUserDefined() {
    return idCategoryTemplate == null;
  }
}
