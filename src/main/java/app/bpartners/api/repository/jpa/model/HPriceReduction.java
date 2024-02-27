package app.bpartners.api.repository.jpa.model;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "\"price_reduction\"")
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HPriceReduction {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private String description;

  @Column(name = "\"value\"")
  private int value;
}
