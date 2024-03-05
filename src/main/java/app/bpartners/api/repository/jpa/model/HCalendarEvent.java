package app.bpartners.api.repository.jpa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "\"calendar_event\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class HCalendarEvent implements Serializable {
  @Id private String id;
  private String eteId;
  private String summary;
  private String organizer;
  private String location;
  private String participants;

  @Column(name = "\"from\"")
  private Instant from;

  @Column(name = "\"to\"")
  private Instant to;

  private Instant updatedAt;
  private Instant createdAt;
  private String idCalendar;
  private String idUser;
  @Transient private boolean sync;
  @Transient private boolean newEvent;
}
