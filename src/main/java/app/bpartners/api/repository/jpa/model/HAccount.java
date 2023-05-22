package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.AccountStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
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
import org.hibernate.annotations.Type;

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
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private String id;
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
  //TODO: It should be updated each time an account is persisted
  private String availableBalance;
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private AccountStatus status;
}