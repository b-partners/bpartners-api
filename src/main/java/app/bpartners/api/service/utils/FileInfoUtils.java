package app.bpartners.api.service.utils;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

@NoArgsConstructor
@Slf4j
public class FileInfoUtils {

  public static final String PDF_EXTENSION = ".pdf";
  public static final String JPEG_EXTENSION = ".jpeg";
  public static final String JPG_EXTENSION = ".jpg";
  public static final String JPG_FORMAT_NAME = "JPG";


  public static MediaType parseMediaTypeFromBytes(String fileId, byte[] bytes) {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
    try {
      String guessedMediaTypeValue = URLConnection.guessContentTypeFromStream(inputStream);
      String fileExtensionMediaType = getFileExtensionMediaTypeValue(fileId);
      if (guessedMediaTypeValue != null && !guessedMediaTypeValue.equals(fileExtensionMediaType)) {
        throw new BadRequestException("File guessed extension and provided extension mismatch");
      }
      String mediaTypeValue = guessedMediaTypeValue == null ? fileExtensionMediaType :
          guessedMediaTypeValue;
      return MediaType.parseMediaType(mediaTypeValue);
    } catch (IOException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }

  private static String getFileExtensionMediaTypeValue(String fileId) {
    switch (getFileExtension(fileId)) {
      case PDF_EXTENSION:
        return MediaType.APPLICATION_PDF_VALUE;
      case JPEG_EXTENSION:
      case JPG_EXTENSION:
        return MediaType.IMAGE_JPEG_VALUE;
      default:
        throw new BadRequestException("Only pdf and jpeg/jpg files are allowed.");
    }
  }

  private static String getFileExtension(String fileId) {
    int indexOfSeparator = fileId.indexOf(".");
    if (indexOfSeparator == -1) {
      throw new BadRequestException("File ID should contain the file extension");
    }
    return fileId.substring(indexOfSeparator);
  }
}