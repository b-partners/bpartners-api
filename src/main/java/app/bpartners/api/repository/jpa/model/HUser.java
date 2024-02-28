package app.bpartners.api.repository.jpa.model;

import static io.hypersistence.utils.hibernate.type.array.internal.AbstractArrayType.SQL_ARRAY_TYPE;
import static jakarta.persistence.GenerationType.IDENTITY;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.IdentificationStatus;
import app.bpartners.api.endpoint.rest.security.model.Role;
import app.bpartners.api.model.BankConnection;
import io.hypersistence.utils.hibernate.type.array.EnumArrayType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "\"user\"")
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
  private String bridgeUserId; // TODO: persist this when creating new users
  private String bridgePassword; // TODO: persist this when creating new users
  private String phoneNumber;
  private String accessToken;
  private Instant tokenExpirationDatetime;
  private Instant tokenCreationDatetime;
  private int monthlySubscription;
  private Long bridgeItemId;
  @CreationTimestamp private Instant bridgeItemUpdatedAt;

  @CreationTimestamp
  @Getter(AccessLevel.NONE)
  private Instant bridgeItemLastRefresh;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private BankConnection.BankConnectionStatus bankConnectionStatus;

  private String logoFileId;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private EnableStatus status;

  private Boolean idVerified;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private IdentificationStatus identificationStatus;

  @Column(name = "old_s3_id_account")
  private String oldS3AccountKey;

  @Type(
      value = EnumArrayType.class,
      parameters = @Parameter(name = SQL_ARRAY_TYPE, value = "user_role"))
  @Column(name = "roles", columnDefinition = "user_role[]")
  private Role[] roles;

  private String snsArn;
  private String deviceToken;

  public Instant getBridgeItemLastRefresh() {
    return bridgeItemLastRefresh == null
        ? null
        : bridgeItemLastRefresh.truncatedTo(ChronoUnit.MILLIS);
  }
}
