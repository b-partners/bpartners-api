package app.bpartners.api.service;

import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.repository.FileRepository;
import app.bpartners.api.service.aws.S3Service;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Service
@AllArgsConstructor
public class FileService {
  private final S3Service s3Service;
  private final FileRepository repository;
  private final FileMapper mapper;

  public byte[] uploadFile(String accountId, String fileId, byte[] toUpload) {
    String checksum =  s3Service.uploadFile(accountId, fileId, toUpload);
    repository.save(mapper.toDomain(fileId, toUpload, checksum));
    return toUpload;
  }

  public byte[] downloadFile(String accountId, String fileId) {
    try {
      return s3Service.downloadFile(accountId, fileId);
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public FileInfo getFileByAccountIdAndId(String accountId, String fileId) {
    return repository.getByAccountIdAndId(accountId, fileId);
  }
}
