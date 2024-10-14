package app.bpartners.api.file;

import static java.util.UUID.randomUUID;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FileZipper implements Function<List<File>, File> {
  private static final String ZIP_FILE_EXTENSION = ".zip";

  @SneakyThrows
  @Override
  public File apply(List<File> fileList) {
    File zipFile = File.createTempFile(randomUUID().toString(), ZIP_FILE_EXTENSION, null);
    try (FileOutputStream fos = new FileOutputStream(zipFile);
        ZipOutputStream zipOut = new ZipOutputStream(fos)) {
      for (File file : fileList) {
        try (FileInputStream fis = new FileInputStream(file)) {
          ZipEntry zipEntry = new ZipEntry(randomUUID().toString());
          zipOut.putNextEntry(zipEntry);
          log.info("DEBUG file {} added to zip", file.getName());
          byte[] bytes = new byte[1024];
          int length;
          while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
          }
        }
      }
    }
    return zipFile;
  }
}
