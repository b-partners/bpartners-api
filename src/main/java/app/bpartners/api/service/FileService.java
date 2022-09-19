package app.bpartners.api.service;

import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.repository.FileRepository;
import app.bpartners.api.service.aws.S3Service;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Service
@AllArgsConstructor
public class FileService {
  private final S3Service s3Service;
  private final FileRepository repository;
  private final FileMapper mapper;

  public byte[] uploadFile(String fileId, byte[] toUpload) {
    byte[] data = null;
    PutObjectResponse objectResponse = s3Service.uploadFile(fileId, toUpload);
    repository.save(mapper.toDomain(fileId, toUpload, objectResponse));
    return data;
  }

  public byte[] downloadFile(String fileId) {
    try {
      return s3Service.downloadFile(fileId);
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public FileInfo getFileById(String fileId) {
    return repository.getById(fileId);
  }

  public String deleteFile(String fileId) {
    s3Service.deleteFile(fileId);
    return fileId;
  }
}
