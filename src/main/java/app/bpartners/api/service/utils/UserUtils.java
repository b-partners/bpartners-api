package app.bpartners.api.service.utils;

import static app.bpartners.api.endpoint.rest.model.FileType.LOGO;

import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.file.FileService;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserUtils {

  private UserUtils() {}

  public static File getUserLogoFile(String userId, String logoFileId, FileService fileService) {

    var logoFile = logoFileId == null ? null : fileService.downloadFile(LOGO, userId, logoFileId);

    if (logoFile == null) {
      log.info("User.{} has no logo", userId);
    }

    return logoFile;
  }

  public static BufferedImage getUserLogo(
      String userId, String userLogoFileId, FileService fileService) {
    try {
      File logoFile = getUserLogoFile(userId, userLogoFileId, fileService);
      return logoFile == null ? null : ImageIO.read(logoFile.toPath().toFile());
    } catch (IOException e) {
      throw new BadRequestException("User logo is not a valid image");
    }
  }
}
