package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import app.bpartners.api.repository.jpa.types.PostgresEnumType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "\"transaction_category_template\"")
@TypeDef(name = "pgsql_enum", typeClass = PostgresEnumType.class)
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HTransactionCategoryTemplate {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;
  @Column(name = "\"type\"")
  private String type;
  private String vat;
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private TransactionTypeEnum transactionType;
  private boolean other;
  private String description;
}
