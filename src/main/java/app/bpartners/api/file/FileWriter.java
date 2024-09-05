package app.bpartners.api.file;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.exception.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FileWriter implements BiFunction<byte[], File, File> {
  private final ObjectMapper objectMapper;
  private final ExtensionGuesser extensionGuesser;

  @Override
  public File apply(byte[] bytes, @Nullable File directory) {
    try {
      String name = randomUUID().toString();
      String suffix = "." + extensionGuesser.apply(bytes);
      File tempFile = File.createTempFile(name, suffix, directory);
      return Files.write(tempFile.toPath(), bytes).toFile();
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public byte[] writeAsByte(Object object) {
    try {
      return objectMapper.writeValueAsBytes(object);
    } catch (JsonProcessingException e) {
      throw new ApiException(SERVER_EXCEPTION, "error during object conversion to bytes");
    }
  }

  public File write(byte[] bytes, @Nullable File directory, String filename) {
    if (directory != null && directory.getName().contains("..")) {
      throw new IllegalArgumentException("name must not contain .. but receceived: pathValue");
    }
    try {
      String suffix = extensionGuesser.apply(bytes);
      File newFile = new File(directory, filename + suffix);
      Files.createDirectories(newFile.toPath().getParent());
      return Files.write(newFile.toPath(), bytes).toFile();
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  @SneakyThrows
  public byte[] writeAsByte(File file) {
    return Files.readAllBytes(file.toPath());
  }

  public static String base64Image(byte[] image) {
    if (image == null) {
      return null;
    }
    return Base64.getEncoder().encodeToString(image);
  }
}
