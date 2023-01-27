package app.bpartners.api.repository.jpa.model;

import java.io.Serializable;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "\"annual_revenue_target\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class HAnnualRevenueTarget implements Serializable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private String id;
  @ManyToOne
  @JoinColumn(name = "id_account_holder")
  private HAccountHolder accountHolder;
  @Column(name = "\"year\"")
  private Integer year;
  private String amountTarget;
  @UpdateTimestamp
  private Instant updatedAt;
}
