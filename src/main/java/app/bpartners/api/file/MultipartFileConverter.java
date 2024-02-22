package app.bpartners.api.file;

import app.bpartners.api.model.exception.ApiException;
import java.io.IOException;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class MultipartFileConverter implements Function<MultipartFile, byte[]> {
  @Override
  public byte[] apply(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (IOException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }
}
