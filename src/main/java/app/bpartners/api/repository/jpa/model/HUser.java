package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.IdentificationStatus;
import app.bpartners.api.endpoint.rest.model.UserRole;
import app.bpartners.api.endpoint.rest.security.model.Role;
import app.bpartners.api.model.BankConnection;
import app.bpartners.api.repository.jpa.types.PostgresEnumType;
import io.hypersistence.utils.hibernate.type.array.EnumArrayType;
import io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import static javax.persistence.GenerationType.IDENTITY;

@Entity
@Table(name = "\"user\"")
@TypeDef(name = "pgsql_enum", typeClass = PostgresEnumType.class)
@TypeDef(
    name = "user_roles",
    typeClass = EnumArrayType.class,
    defaultForType = UserRole[].class,
    parameters = @Parameter(
        name = AbstractArrayType.SQL_ARRAY_TYPE,
        value = "user_role"
    )
)
@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HUser implements Serializable {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;
  @OneToMany(mappedBy = "user")
  private List<HAccount> accounts;
  @OneToMany
  @JoinColumn(name = "id_user")
  private List<HAccountHolder> accountHolders;
  private String firstName;
  private String lastName;
  @Column(name = "preferred_account_id")
  private String preferredAccountId;
  private String email;
  private String bridgeUserId; //TODO: persist this when creating new users
  private String bridgePassword; //TODO: persist this when creating new users
  private String phoneNumber;
  private String accessToken;
  private Instant tokenExpirationDatetime;
  private Instant tokenCreationDatetime;
  private int monthlySubscription;
  private Long bridgeItemId;
  @CreationTimestamp
  private Instant bridgeItemUpdatedAt;
  @CreationTimestamp
  @Getter(AccessLevel.NONE)
  private Instant bridgeItemLastRefresh;
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private BankConnection.BankConnectionStatus bankConnectionStatus;
  private String logoFileId;
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private EnableStatus status;
  private Boolean idVerified;
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private IdentificationStatus identificationStatus;
  @Column(name = "old_s3_id_account")
  private String oldS3AccountKey;
  @Type(type = "user_roles")
  @Column(name = "roles", columnDefinition = "user_role[]")
  private Role[] roles;
  private String snsArn;

  public Instant getBridgeItemLastRefresh() {
    return bridgeItemLastRefresh == null ? null
        : bridgeItemLastRefresh.truncatedTo(ChronoUnit.MILLIS);
  }
}