package app.bpartners.api.service.utils;

import app.bpartners.api.model.exception.BadRequestException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.http.MediaType;

import static app.bpartners.api.service.CustomerService.EXCEL_MIME_TYPE;

@Slf4j
@NoArgsConstructor
public class FileInfoUtils {

  public static final String PDF_EXTENSION = ".pdf";
  public static final String JPG_FORMAT_NAME = "JPG";


  public static MediaType parseMediaTypeFromBytes(byte[] bytes) {
    Tika tika = new Tika();
    String guessedMediaTypeValue = tika.detect(bytes);
    checkValidMediaType(guessedMediaTypeValue);
    return MediaType.parseMediaType(guessedMediaTypeValue);
  }

  public static MediaType parseMediaTypeFromBytesWithoutCheck(byte[] bytes) {
    Tika tika = new Tika();
    String guessedMediaTypeValue = tika.detect(bytes);
    return MediaType.parseMediaType(guessedMediaTypeValue);
  }

  private static void checkValidMediaType(String mediaType) {
    if (!mediaType.equals(MediaType.IMAGE_JPEG_VALUE)
        && !mediaType.equals(MediaType.IMAGE_PNG_VALUE)
        && !mediaType.equals(MediaType.APPLICATION_PDF_VALUE)
        && !mediaType.equals(EXCEL_MIME_TYPE)) {
      throw new BadRequestException("Only pdf, png, jpeg/jpg and excel files are allowed.");
    }
  }
}