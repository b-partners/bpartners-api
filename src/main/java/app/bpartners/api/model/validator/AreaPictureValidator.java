package app.bpartners.api.model.validator;

import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AreaPictureValidator implements Consumer<AreaPicture> {
  private static final Pattern AREA_PICTURE_FILENAME_PATTERN = Pattern.compile("");

  @Override
  public void accept(AreaPicture areaPicture) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();

    String filename = areaPicture.getFilename();
    if (areaPicture.getAddress() == null) {
      exceptionMessageBuilder.append("address is mandatory. ");
    }
    if (areaPicture.getLayer() == null) {
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
    } /* else {
        Matcher matcher = AREA_PICTURE_FILENAME_PATTERN.matcher(filename);
        if (!matcher.matches()) {
          exceptionMessageBuilder
              .append("filename does not match pattern: ")
              .append(AREA_PICTURE_FILENAME_PATTERN)
              .append(".");
        }
      }*/

    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
