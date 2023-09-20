package app.bpartners.api.model;

import com.google.api.services.calendar.model.Event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class EventConnector {
  private String domainId;
  private Event googleEvent;
}
