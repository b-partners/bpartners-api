package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.AreaPictureAnnotationInstance;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AreaPictureAnnotationInstanceRestValidator
    implements Consumer<AreaPictureAnnotationInstance> {

  @Override
  public void accept(AreaPictureAnnotationInstance annotation) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (annotation.getId() == null) {
      exceptionMessageBuilder.append("id is mandatory");
    }
    if (annotation.getLabelName() == null) {
      exceptionMessageBuilder.append("labelName is mandatory");
    }
    if (annotation.getUserId() == null) {
      exceptionMessageBuilder.append("userId is mandatory");
    }
    if (annotation.getAreaPictureId() == null) {
      exceptionMessageBuilder.append("areaPictureId is mandatory");
    }
    if (annotation.getAnnotationId() == null) {
      exceptionMessageBuilder.append("annotationId is mandatory");
    }
    if (annotation.getPolygon() == null) {
      exceptionMessageBuilder.append("polygon is mandatory");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isBlank()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
