package app.bpartners.api.service.file;

import static app.bpartners.api.endpoint.rest.model.FileType.LOGO;

import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.repository.FileRepository;
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
  private final FileWriter fileWriter;
  private final LogoService logoService;

  public FileInfo upload(FileType fileType, String fileId, String userId, File file) {
    if (fileType.equals(LOGO)) {
      return logoService.compressUserLogo(userId, file, fileId);
    }

    String sha256 = s3Service.uploadFile(fileType, fileId, userId, file).value();
    var filesAsBytes = fileWriter.writeAsByte(file);
    return repository.save(mapper.toDomain(fileId, filesAsBytes, sha256, userId));
  }

  public File downloadFile(FileType fileType, String userId, String fileId) {
    if (repository.findOptionalById(fileId).isEmpty()) {
      throw new NotFoundException("File." + fileId + " not found.");
    }

    return s3Service.downloadFile(fileType, fileId, userId);
  }

  public FileInfo findById(String fileId) {
    return repository.findById(fileId);
  }

  public File downloadLandingFile(String key) {
    return s3Service.downloadLandingFile(key);
  }

  public String uploadLandingFile(File file, String key) {
    return s3Service.uploadLandingFile(file, key);
  }
}
