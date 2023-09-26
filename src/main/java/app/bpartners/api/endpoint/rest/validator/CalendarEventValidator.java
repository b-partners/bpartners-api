package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateCalendarEvent;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CalendarEventValidator implements Consumer<CreateCalendarEvent> {
  @Override
  public void accept(CreateCalendarEvent createCalendarEvent) {
    StringBuilder exceptionBuilder = new StringBuilder();
    if (createCalendarEvent == null) {
      exceptionBuilder.append("CreateCalendarEvent is mandatory");
    } else {
      if (createCalendarEvent.getId() == null) {
        exceptionBuilder.append("Id is mandatory. ");
      }
      if (createCalendarEvent.getSummary() == null) {
        exceptionBuilder.append("Summary is mandatory. ");
      }
      if (createCalendarEvent.getFrom() == null) {
        exceptionBuilder.append("From is mandatory. ");
        if (createCalendarEvent.getTo() == null) {
          exceptionBuilder.append("To is mandatory. ");
        }
      }
    }
    String exceptionMsg = exceptionBuilder.toString();
    if (!exceptionMsg.isEmpty()) {
      throw new BadRequestException(exceptionMsg);
    }
  }
}
