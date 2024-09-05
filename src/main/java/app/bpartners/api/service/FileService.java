package app.bpartners.api.service;

import static app.bpartners.api.endpoint.rest.model.FileType.LOGO;

import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.repository.FileRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.service.aws.S3Service;
import java.io.File;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FileService {
  private final S3Service s3Service;
  private final FileRepository repository;
  private final FileMapper mapper;
  private final UserJpaRepository userJpaRepository;
  private final FileWriter fileWriter;

  public FileInfo upload(FileType fileType, String fileId, String idUser, File file) {
    String sha256 = s3Service.uploadFile(fileType, fileId, idUser, file).value();
    if (fileType.equals(LOGO)) {
      saveUserFileId(fileId, idUser);
    }
    var filesAsBytes = fileWriter.writeAsByte(file);
    return repository.save(mapper.toDomain(fileId, filesAsBytes, sha256, idUser));
  }

  public File downloadFile(FileType fileType, String idUser, String fileId) {
    if (repository.findOptionalById(fileId).isEmpty()) {
      throw new NotFoundException("File." + fileId + " not found.");
    }
    return s3Service.downloadFile(fileType, fileId, idUser);
  }

  public FileInfo findById(String fileId) {
    return repository.findById(fileId);
  }

  private void saveUserFileId(String fileId, String idUser) {
    HUser entity = userJpaRepository.getById(idUser).toBuilder().logoFileId(fileId).build();
    userJpaRepository.save(entity);
  }
}
