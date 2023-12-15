package app.bpartners.api.service.utils;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.CLIENT_EXCEPTION;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;

public class FileUtils {
  private FileUtils() {}

  public static String base64Image(byte[] image) {
    if (image == null) {
      return null;
    }
    return Base64.getEncoder().encodeToString(image);
  }

  public static byte[] toByteArray(File file) {
    try (FileInputStream fileInputStream = new FileInputStream(file); ) {
      int fileSize = (int) file.length();
      byte[] result = new byte[fileSize];
      int readBytes = fileInputStream.read(result);
      if (fileSize != readBytes) {
        throw new ApiException(
            CLIENT_EXCEPTION, "File" + file.getName() + " could not be entirely read. ");
      }
      return result;
    } catch (IOException e) {
      throw new BadRequestException(e.getMessage());
    }
  }
}
