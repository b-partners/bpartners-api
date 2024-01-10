package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.repository.jpa.types.PostgresEnumType;
import java.time.Instant;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.TypeDef;

@Entity
@Table(name = "\"feedback\"")
@TypeDef(name = "pgsql_enum", typeClass = PostgresEnumType.class)
@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HFeedback {
  @Id private String id;

  @ManyToOne
  @JoinColumn(name = "account_holder_id")
  private HAccountHolder accountHolder;

  @ManyToMany
  @JoinTable(
      name = "customer_feedback",
      joinColumns = @JoinColumn(name = "feedback_id"),
      inverseJoinColumns = @JoinColumn(name = "customer_id"))
  private List<HCustomer> customers;

  private Instant creationDatetime;
}
