package app.bpartners.api.repository.jpa.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "\"feedback\"")
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
