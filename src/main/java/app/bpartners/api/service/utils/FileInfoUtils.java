package app.bpartners.api.service.utils;

import static app.bpartners.api.service.CustomerService.EXCEL_MIME_TYPE;

import app.bpartners.api.model.exception.BadRequestException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.http.MediaType;

@Slf4j
@NoArgsConstructor
public class FileInfoUtils {

  public static final String PDF_EXTENSION = ".pdf";
  public static final String JPG_FORMAT_NAME = "JPG";
  public static final String APPLICATION_ZIP_VALUE = "application/zip";

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
        && !mediaType.equals(EXCEL_MIME_TYPE)
        && !mediaType.equals(APPLICATION_ZIP_VALUE)) {
      throw new BadRequestException("Only pdf, png, jpeg/jpg, zip and excel files are allowed.");
    }
  }
}
