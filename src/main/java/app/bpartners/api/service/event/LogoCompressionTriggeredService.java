package app.bpartners.api.service.event;

import static app.bpartners.api.service.utils.UserUtils.getUserLogoFile;

import app.bpartners.api.endpoint.event.model.LogoCompressionTriggered;
import app.bpartners.api.service.file.FileService;
import app.bpartners.api.service.file.LogoService;
import java.io.File;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogoCompressionTriggeredService implements Consumer<LogoCompressionTriggered> {
  private final LogoService logoService;
  private final FileService fileService;

  @Override
  public void accept(LogoCompressionTriggered logoCompressionTriggered) {
    String userId = logoCompressionTriggered.getUserId();
    String logoFileId = logoCompressionTriggered.getUserLogoFileId();

    if (logoFileId == null) {
      log.warn("User.{} has no logo", userId);
      return;
    } else if (logoService.isCompressedLogo(logoFileId)) {
      log.info("User.{} already has a compressed logo", userId);
      return;
    }

    File logoFile = getUserLogoFile(userId, logoFileId, fileService);
    logoService.compressUserLogo(userId, logoFile, logoFileId);
  }
}
