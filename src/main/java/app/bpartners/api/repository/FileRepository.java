package app.bpartners.api.repository;


import app.bpartners.api.model.FileInfo;
import java.util.Optional;

public interface FileRepository {
  FileInfo findById(String id);

  Optional<FileInfo> findOptionalById(String id);

  FileInfo save(FileInfo file);
}
