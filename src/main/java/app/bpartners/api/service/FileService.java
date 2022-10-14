package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.repository.FileRepository;
import app.bpartners.api.service.aws.S3Service;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class FileService {
  private final S3Service s3Service;
  private final FileRepository repository;
  private final FileMapper mapper;

  public void uploadFile(FileType fileType, String accountId, String fileId, byte[] toUpload) {
    String checksum = s3Service.uploadFile(fileType, accountId, fileId, toUpload);
    repository.save(mapper.toDomain(fileId, toUpload, checksum));
  }

  public byte[] downloadFile(FileType fileType, String accountId, String fileId) {
    getFileByAccountIdAndId(accountId, fileId);
    return s3Service.downloadFile(fileType, accountId, fileId);
  }

  public FileInfo getFileByAccountIdAndId(String accountId, String fileId) {
    return repository.getByAccountIdAndId(accountId, fileId);
  }
}
