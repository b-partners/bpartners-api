package app.bpartners.api.repository.jpa.model;

import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.api.endpoint.rest.model.AccountStatus;
import app.bpartners.api.endpoint.rest.model.EnableStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "\"account\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class HAccount implements Serializable {
  @Id private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_user")
  @JsonIgnore
  private HUser user;

  private String idBank;
  private String externalId;

  @Column(name = "\"name\"")
  private String name;

  private String iban;
  private String bic;
  // TODO: It should be updated each time an account is persisted
  private String availableBalance;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private AccountStatus status;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private EnableStatus enableStatus;
}
