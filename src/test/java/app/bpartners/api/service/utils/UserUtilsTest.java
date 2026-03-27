package app.bpartners.api.service.utils;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.file.FileService;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class UserUtilsTest {

  FileService fileService = mock();

  @Test
  void get_user_logo() {
    String userId = randomUUID().toString();
    String logoFileId = randomUUID().toString();
    File logoFile = mock();
    when(fileService.downloadFile(any(), any(), any())).thenReturn(logoFile);

    File actual = UserUtils.getUserLogoFile(userId, logoFileId, fileService);

    assertEquals(logoFile, actual);
  }

  @Test
  void get_user_logo_buffered_image() throws IOException {
    String userId = randomUUID().toString();
    String logoFileId = randomUUID().toString();
    File logoFile = new ClassPathResource("files/logo_company.jpeg").getFile();
    when(fileService.downloadFile(any(), any(), any())).thenReturn(logoFile);

    BufferedImage actual = UserUtils.getUserLogo(userId, logoFileId, fileService);

    assertNotNull(actual);
    assertEquals(1200, actual.getWidth());
    assertEquals(1200, actual.getHeight());
  }

  @Test
  void get_user_logo_buffered_image_null() {
    String userId = randomUUID().toString();
    String logoFileId = randomUUID().toString();
    when(fileService.downloadFile(any(), any(), any())).thenReturn(null);

    BufferedImage actual = UserUtils.getUserLogo(userId, logoFileId, fileService);

    assertNull(actual);
  }

  @Test
  void get_user_logo_buffered_image_invalid_image() {
    String userId = randomUUID().toString();
    String logoFileId = randomUUID().toString();
    File logoFile = mock();
    when(logoFile.toPath()).thenReturn(Path.of("non-existent-file"));
    when(fileService.downloadFile(any(), any(), any())).thenReturn(logoFile);

    assertThrows(
        BadRequestException.class, () -> UserUtils.getUserLogo(userId, logoFileId, fileService));
  }

  @Test
  void user_has_no_logo() {
    String userId = randomUUID().toString();
    String logoFileId = randomUUID().toString();
    when(fileService.downloadFile(any(), any(), any())).thenReturn(null);

    File actual = UserUtils.getUserLogoFile(userId, logoFileId, fileService);

    assertNull(actual);
  }
}
