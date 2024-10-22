package app.bpartners.api.file;

import static app.bpartners.api.service.event.InvoiceExportLinkRequestedService.PDF_FILE_EXTENSION;
import static java.util.UUID.randomUUID;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
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
          ZipEntry zipEntry = new ZipEntry(file.getName());
          zipOut.putNextEntry(zipEntry);
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
