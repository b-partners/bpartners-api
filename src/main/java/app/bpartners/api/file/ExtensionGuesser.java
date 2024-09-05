package app.bpartners.api.file;

import java.util.function.Function;
import lombok.SneakyThrows;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypes;
import org.springframework.stereotype.Component;

@Component
public class ExtensionGuesser implements Function<byte[], String> {
  @SneakyThrows
  @Override
  public String apply(byte[] bytes) {
    var tika = new Tika();
    String detectedMediaType = tika.detect(bytes);
    String extension = MimeTypes.getDefaultMimeTypes().forName(detectedMediaType).getExtension();
    return extension.startsWith(".") ? extension : "." + extension;
  }
}
