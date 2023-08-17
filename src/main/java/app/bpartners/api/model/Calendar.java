package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.CalendarPermission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Calendar {
  private String id;
  private String eteId;
  //TODO: private User owner;
  private String summary;
  private CalendarPermission calendarPermission;
}
