package app.bpartners.api.repository.jpa.model;

import static jakarta.persistence.GenerationType.IDENTITY;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.api.endpoint.rest.model.CalendarPermission;
import java.time.Instant;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "\"calendar\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode
public class HCalendar {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private String idUser;
  private String eteId;
  private String summary;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private CalendarPermission permission;

  private Instant createdAt;
  @Transient private boolean newCalendar;
}
