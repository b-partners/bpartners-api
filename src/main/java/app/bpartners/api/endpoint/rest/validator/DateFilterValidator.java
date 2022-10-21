package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.model.exception.BadRequestException;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class DateFilterValidator {
  public void accept(LocalDate startDate, LocalDate endDate) {
    if (!startDate.isEqual(endDate) && startDate.isAfter(endDate)) {
      throw new BadRequestException("The start date cannot be after the end date");
    }
  }
}
