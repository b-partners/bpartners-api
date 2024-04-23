package app.bpartners.api.model.validator;

import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class AreaPictureValidator implements Consumer<AreaPicture> {

  @Override
  public void accept(AreaPicture areaPicture) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();

    String filename = areaPicture.getFilename();
    if (areaPicture.getAddress() == null) {
      exceptionMessageBuilder.append("address is mandatory. ");
    }
    if (areaPicture.getCurrentLayer() == null) {
      exceptionMessageBuilder.append("layer is mandatory. ");
    }
    if (areaPicture.getIdFileInfo() == null) {
      exceptionMessageBuilder.append("fileId is mandatory. ");
    }
    if (areaPicture.getZoomLevel() == null) {
      exceptionMessageBuilder.append("zoomLevel is mandatory. ");
    }
    if (filename == null) {
      exceptionMessageBuilder.append("filename is mandatory. ");
    }
    if (areaPicture.getIdProspect() == null) {
      exceptionMessageBuilder.append("prospectId is mandatory. ");
    }

    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
