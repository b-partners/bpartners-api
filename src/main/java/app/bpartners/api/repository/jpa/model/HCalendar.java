package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.CalendarPermission;
import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;

import static javax.persistence.GenerationType.IDENTITY;

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
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private CalendarPermission permission;
  private Instant createdAt;
  @Transient
  private boolean newCalendar;
}
