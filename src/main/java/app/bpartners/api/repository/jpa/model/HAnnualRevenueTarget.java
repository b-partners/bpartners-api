package app.bpartners.api.repository.jpa.model;

import java.io.Serializable;
import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class HAnnualRevenueTarget implements Serializable {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private String id;

  private String idAccountHolder;

  @Column(name = "\"year\"")
  private Integer year;

  private String amountTarget;
  @UpdateTimestamp private Instant updatedAt;
}
