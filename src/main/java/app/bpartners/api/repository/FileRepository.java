package app.bpartners.api.repository;


import app.bpartners.api.model.FileInfo;
import java.util.Optional;

public interface FileRepository {
  FileInfo getByAccountIdAndId(String accountId, String id);

  FileInfo getById(String id);

  Optional<FileInfo> getOptionalByIdAndAccountId(String id, String accountId);

  FileInfo save(FileInfo file);
}
