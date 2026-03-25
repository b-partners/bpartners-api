package app.bpartners.api.service.event;

import static app.bpartners.api.service.utils.UserUtils.getUserLogoFile;

import app.bpartners.api.endpoint.event.model.LogoCompressionTriggered;
import app.bpartners.api.model.User;
import app.bpartners.api.service.file.FileService;
import app.bpartners.api.service.file.LogoService;
import java.io.File;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class LogoCompressionTriggeredService implements Consumer<LogoCompressionTriggered> {
  private final LogoService logoService;
  private final FileService fileService;

  @Override
  public void accept(LogoCompressionTriggered logoCompressionTriggered) {
    User user = logoCompressionTriggered.getUser();
    String userLogoId = user.getLogoFileId();

    if (userLogoId == null) {
      log.warn("User {} has no logo", user.getEmail());
      return;
    } else if (logoService.isCompressedLogo(userLogoId)) {
      log.info("User {} already has a compressed logo", user.getEmail());
      return;
    }

    File logoFile = getUserLogoFile(user, fileService);
    logoService.compressUserLogo(user, logoFile, userLogoId);
  }
}
