package app.bpartners.api.repository.jpa.model;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

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
  private String id;
  private String idAccount;
  private String description;
  private String unitPrice;
  private String vatPercent;
  @Column(name = "created_datetime")
  private Instant createdAt;
}
