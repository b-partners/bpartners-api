package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CrupdateAreaPictureDetails;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CrupdateAreaPictureDetailsValidator implements Consumer<CrupdateAreaPictureDetails> {

  @Override
  public void accept(CrupdateAreaPictureDetails crupdateAreaPictureDetails) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();

    String filename = crupdateAreaPictureDetails.getFilename();
    if (crupdateAreaPictureDetails.getAddress() == null) {
      exceptionMessageBuilder.append("address is mandatory. ");
    }
    if (crupdateAreaPictureDetails.getFileId() == null) {
      exceptionMessageBuilder.append("fileId is mandatory. ");
    }
    if (crupdateAreaPictureDetails.getZoomLevel() == null) {
      exceptionMessageBuilder.append("zoomLevel is mandatory. ");
    }
    if (filename == null) {
      exceptionMessageBuilder.append("filename is mandatory. ");
    }
    if (crupdateAreaPictureDetails.getProspectId() == null) {
      exceptionMessageBuilder.append("prospectId is mandatory. ");
    }

    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
