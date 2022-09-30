package app.bpartners.api.service;

import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.repository.FileRepository;
import app.bpartners.api.service.aws.S3Service;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FileService {
  private final S3Service s3Service;
  private final FileRepository repository;
  private final FileMapper mapper;

  public void uploadFile(String accountId, String fileId, byte[] toUpload) {
    String checksum = s3Service.uploadFile(accountId, fileId, toUpload);
    repository.save(mapper.toDomain(fileId, toUpload, checksum));
  }

  public byte[] downloadFile(String accountId, String fileId) {
    return s3Service.downloadFile(accountId, fileId);
  }

  public FileInfo getFileByAccountIdAndId(String accountId, String fileId) {
    return repository.getByAccountIdAndId(accountId, fileId);
  }
}
