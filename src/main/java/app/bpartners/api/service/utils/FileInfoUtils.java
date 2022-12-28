package app.bpartners.api.service.utils;

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


  public static MediaType parseMediaTypeFromBytes(byte[] bytes) {
    Tika tika = new Tika();
    String guessedMediaTypeValue = tika.detect(bytes);
    checkValidMediaType(guessedMediaTypeValue);
    return MediaType.parseMediaType(guessedMediaTypeValue);
  }

  private static void checkValidMediaType(String mediaType) {
    if (!mediaType.equals(MediaType.IMAGE_JPEG_VALUE)
        && !mediaType.equals(MediaType.IMAGE_PNG_VALUE)
        && !mediaType.equals(MediaType.APPLICATION_PDF_VALUE)) {
      throw new BadRequestException("Only pdf, png and jpeg/jpg files are allowed.");
    }
  }
}