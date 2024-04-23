package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.Point;
import app.bpartners.api.endpoint.rest.model.Polygon;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class PolygonRestValidator implements Consumer<Polygon> {

  @Override
  public void accept(Polygon polygon) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    List<Point> points = polygon.getPoints();
    if (points == null || points.isEmpty()) {
      exceptionMessageBuilder.append("polygon points attribute is mandatory");
    } else if (points.getFirst() != points.getLast()) {
      exceptionMessageBuilder.append("polygon is not complete, first and last points differ");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isBlank()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
