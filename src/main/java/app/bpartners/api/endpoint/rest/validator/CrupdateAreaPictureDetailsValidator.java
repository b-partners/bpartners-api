package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CrupdateAreaPictureDetails;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class CrupdateAreaPictureDetailsValidator implements Consumer<CrupdateAreaPictureDetails> {
  private static final Pattern AREA_PICTURE_FILENAME_PATTERN = Pattern.compile("");

  @Override
  public void accept(CrupdateAreaPictureDetails crupdateAreaPictureDetails) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();

    String filename = crupdateAreaPictureDetails.getFilename();
    if (crupdateAreaPictureDetails.getAddress() == null) {
      exceptionMessageBuilder.append("address is mandatory. ");
    }
    if (crupdateAreaPictureDetails.getLayer() == null) {
      exceptionMessageBuilder.append("layer is mandatory. ");
    }
    if (crupdateAreaPictureDetails.getFileId() == null) {
      exceptionMessageBuilder.append("fileId is mandatory. ");
    }
    if (crupdateAreaPictureDetails.getZoomLevel() == null) {
      exceptionMessageBuilder.append("zoomLevel is mandatory. ");
    }
    if (filename == null) {
      exceptionMessageBuilder.append("filename is mandatory. ");
    } /*else {
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
