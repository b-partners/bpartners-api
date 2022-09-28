package app.bpartners.api.service.utils;

import app.bpartners.api.model.exception.ApiException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import lombok.NoArgsConstructor;
import org.springframework.http.MediaType;

@NoArgsConstructor
public class FileInfoUtils {
  public static MediaType parseMediaTypeFromBytes(byte[] bytes) {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
    try {
      return MediaType.parseMediaType(URLConnection.guessContentTypeFromStream(inputStream));
    } catch (IOException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }
}