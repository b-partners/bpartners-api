package app.bpartners.api.model.validator;

import app.bpartners.api.model.AreaPictureAnnotationInstance;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class PolygonValidator implements Consumer<AreaPictureAnnotationInstance.Polygon> {

  @Override
  public void accept(AreaPictureAnnotationInstance.Polygon polygon) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    List<AreaPictureAnnotationInstance.Point> points = polygon.points();
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
