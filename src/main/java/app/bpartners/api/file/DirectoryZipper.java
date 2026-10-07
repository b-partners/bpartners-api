package app.bpartners.api.file;

import static java.util.UUID.randomUUID;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
public class DirectoryZipper implements Function<Path, File> {
  private static final String ZIP_FILE_EXTENSION = ".zip";

  @SneakyThrows
  @Override
  public File apply(Path rootDirectory) {
    File zipFile = File.createTempFile(randomUUID().toString(), ZIP_FILE_EXTENSION, null);
    List<Path> entries;
    try (var tree = Files.walk(rootDirectory)) {
      entries = tree.filter(Files::isRegularFile).sorted().toList();
    }
    try (var zipOut = new ZipOutputStream(new FileOutputStream(zipFile))) {
      for (Path entry : entries) {
        zipOut.putNextEntry(new ZipEntry(zipEntryNameOf(rootDirectory, entry)));
        Files.copy(entry, zipOut);
        zipOut.closeEntry();
      }
    }
    return zipFile;
  }

  private String zipEntryNameOf(Path rootDirectory, Path entry) {
    return rootDirectory.relativize(entry).toString().replace(File.separatorChar, '/');
  }
}
