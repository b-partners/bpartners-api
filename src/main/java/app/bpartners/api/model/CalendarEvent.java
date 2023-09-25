package app.bpartners.api.model;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
@EqualsAndHashCode
public class CalendarEvent {
  private String id;
  private String summary;
  private String organizer;
  private String location;
  private List<String> participants;
  private boolean sync = false;
  private ZonedDateTime from;
  private ZonedDateTime to;
  private Instant updatedAt;
  private String eteId; //TODO: deprecated
  private boolean newEvent = false;
}
