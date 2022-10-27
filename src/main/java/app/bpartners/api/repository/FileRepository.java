package app.bpartners.api.repository;


import app.bpartners.api.model.FileInfo;

public interface FileRepository {
  FileInfo getByAccountIdAndId(String accountId, String id);

  FileInfo getById(String id);

  FileInfo save(FileInfo file);
}
