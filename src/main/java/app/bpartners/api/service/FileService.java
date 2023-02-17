package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.repository.FileRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.service.aws.S3Service;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FileService {
  private final S3Service s3Service;
  private final FileRepository repository;
  private final FileMapper mapper;
  private final UserJpaRepository userJpaRepository;

  public FileInfo upload(String fileId, FileType fileType, String accountId, byte[] fileAsBytes,
                         String userId) {
    String sha256 = s3Service.uploadFile(fileType, accountId, fileId, fileAsBytes);
    if (fileType.equals(FileType.LOGO)) {
      saveUserFileId(fileId, userId);
    }
    return repository.save(mapper.toDomain(fileId, fileAsBytes, sha256, accountId));
  }

  public byte[] downloadFile(FileType fileType, String accountId, String fileId) {
    if (repository.getOptionalByIdAndAccountId(fileId, accountId).isEmpty()) {
      throw new NotFoundException("File." + fileId + " not found.");
    } else {
      return s3Service.downloadFile(fileType, accountId, fileId);
    }
  }

  public List<byte[]> downloadOptionalFile(FileType fileType, String accountId, String fileId) {
    if (repository.getOptionalByIdAndAccountId(fileId, accountId).isEmpty()) {
      return List.of();
    } else {
      return List.of(s3Service.downloadFile(fileType, accountId, fileId));
    }
  }

  public FileInfo getFileByAccountIdAndId(String accountId, String fileId) {
    return repository.getByAccountIdAndId(accountId, fileId);
  }

  private void saveUserFileId(String fileId, String userId) {
    HUser entity = userJpaRepository.getById(userId);
    entity.setLogoFileId(fileId);
    userJpaRepository.save(entity);
  }
}
