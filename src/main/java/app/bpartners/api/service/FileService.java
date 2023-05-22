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

  public FileInfo upload(String fileId, FileType fileType, String idUser, byte[] fileAsBytes) {
    String sha256 = s3Service.uploadFile(fileType, idUser, fileId, fileAsBytes);
    if (fileType.equals(FileType.LOGO)) {
      saveUserFileId(fileId, idUser);
    }
    return repository.save(mapper.toDomain(fileId, fileAsBytes, sha256, idUser));
  }

  public byte[] downloadFile(FileType fileType, String idUser, String fileId) {
    if (repository.findOptionalById(fileId).isEmpty()) {
      throw new NotFoundException("File." + fileId + " not found.");
    }
    return s3Service.downloadFile(fileType, idUser, fileId);
  }

  public List<byte[]> downloadOptionalFile(FileType fileType, String idUser, String fileId) {
    return fileId == null || repository.findOptionalById(fileId).isEmpty() ? List.of()
        : List.of(s3Service.downloadFile(fileType, idUser, fileId));
  }

  public FileInfo findById(String fileId) {
    return repository.findById(fileId);
  }

  private void saveUserFileId(String fileId, String idUser) {
    HUser entity = userJpaRepository.getById(idUser).toBuilder()
        .logoFileId(fileId)
        .build();
    userJpaRepository.save(entity);
  }
}
