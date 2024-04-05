package app.bpartners.api.model.validator;

import app.bpartners.api.model.AreaPictureAnnotationInstance;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AreaPictureAnnotationInstanceValidator
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
    if (annotation.getIdUser() == null) {
      exceptionMessageBuilder.append("idUser is mandatory");
    }
    if (annotation.getIdAreaPicture() == null) {
      exceptionMessageBuilder.append("idAreaPicture is mandatory");
    }
    if (annotation.getIdAnnotation() == null) {
      exceptionMessageBuilder.append("idAnnotation is mandatory");
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
