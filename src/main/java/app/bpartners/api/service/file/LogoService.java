package app.bpartners.api.service.file;

import static app.bpartners.api.endpoint.rest.model.FileType.LOGO;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.LogoCompressionTriggered;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.repository.FileRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.service.annotation.ImageCompressor;
import app.bpartners.api.service.aws.S3Service;
import java.io.File;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoService {
  private static final int DEFAULT_LOGO_TARGET_SIZE = 250_000;
  private static final int DEFAULT_MAX_LOGO_WIDTH = 1024;
  private static final int DEFAULT_MAX_LOGO_HEIGHT = 1024;
  private static final String COMPRESSED_LOGO_FILE_PREFIX = "compressed_logo_";
  private final ImageCompressor imageCompressor =
      new ImageCompressor(
          DEFAULT_LOGO_TARGET_SIZE, DEFAULT_MAX_LOGO_WIDTH, DEFAULT_MAX_LOGO_HEIGHT);
  private final FileRepository fileRepository;
  private final FileWriter fileWriter;
  private final FileMapper fileMapper;
  private final S3Service s3Service;
  private final UserJpaRepository userJpaRepository;
  private final EventProducer eventProducer;

  public void triggerLogoCompression(String userId, String userLogoId) {
    eventProducer.accept(List.of(new LogoCompressionTriggered(userId, userLogoId)));
  }

  public FileInfo compressUserLogo(String userId, File logoFile, String logoFileId) {
    String compressedLogoFileId = COMPRESSED_LOGO_FILE_PREFIX + logoFileId;
    return compressUserLogoWithoutIdChange(userId, logoFile, compressedLogoFileId);
  }

  public FileInfo compressUserLogoWithoutIdChange(String userId, File logoFile, String logoFileId) {
    File compressedLogo = imageCompressor.compressLogoFile(logoFile);

    FileInfo savedLogoFileInfo = saveUserLogoFile(userId, logoFileId, compressedLogo);
    log.info("User.{} logo compressed : \nold : {}\nnew : {}", userId, logoFileId, logoFileId);

    return savedLogoFileInfo;
  }

  public boolean isCompressedLogo(String fileId) {
    return fileId.startsWith(COMPRESSED_LOGO_FILE_PREFIX);
  }

  private FileInfo saveUserLogoFile(
      String userId, String compressedLogoFileId, File compressedLogo) {
    FileInfo savedLogoFileInfo = saveFile(userId, compressedLogoFileId, compressedLogo);
    updateUserFileId(userId, compressedLogoFileId);
    return savedLogoFileInfo;
  }

  private FileInfo saveFile(String userId, String fileId, File file) {
    String sha256 = s3Service.uploadFile(LOGO, fileId, userId, file).value();
    var filesAsBytes = fileWriter.writeAsByte(file);
    return fileRepository.save(fileMapper.toDomain(fileId, filesAsBytes, sha256, userId));
  }

  private void updateUserFileId(String userId, String fileId) {
    HUser entity = userJpaRepository.getById(userId).toBuilder().logoFileId(fileId).build();
    userJpaRepository.save(entity);
  }
}
