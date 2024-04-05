package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.Point;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class PointRestValidator implements Consumer<Point> {

  @Override
  public void accept(Point point) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (point.getX() == null) {
      exceptionMessageBuilder.append("point.X is mandatory");
    }
    if (point.getY() == null) {
      exceptionMessageBuilder.append("point.Y is mandatory");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isBlank()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
