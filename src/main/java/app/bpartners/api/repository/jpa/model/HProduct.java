package app.bpartners.api.repository.jpa.model;

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

import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "\"product_template\"")
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HProduct {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;
  private String idAccount;
  private String description;
  private String unitPrice;
  private String vatPercent;
  @CreationTimestamp
  @Column(name = "created_datetime")
  private Instant createdAt;
}
