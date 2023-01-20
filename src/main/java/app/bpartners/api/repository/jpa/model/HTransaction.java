package app.bpartners.api.repository.jpa.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "\"transaction\"")
@Setter
@ToString
@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class HTransaction {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;
  private String idSwan;
  private String idAccount;
}
