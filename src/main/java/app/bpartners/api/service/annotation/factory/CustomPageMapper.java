package app.bpartners.api.service.annotation.factory;

import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.service.annotation.model.custompage.CustomPage;
import app.bpartners.api.service.annotation.model.custompage.PageSection;
import app.bpartners.api.service.annotation.model.custompage.SectionPriority;
import app.bpartners.api.service.annotation.model.custompage.TableData;
import app.bpartners.api.service.annotation.utils.ImageUriUtils;
import app.bpartners.api.service.file.FileService;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Maps the REST custom pages to the template model, resolving their images. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomPageMapper {
  private final FileService fileService;

  public List<CustomPage> map(List<app.bpartners.api.endpoint.rest.model.CustomPage> customPages) {
    return customPages.stream()
        .map(
            page ->
                CustomPage.builder()
                    .pageTitle(page.getPageTitle())
                    .sections(page.getSections().stream().map(this::mapSection).toList())
                    .build())
        .toList();
  }

  private PageSection mapSection(app.bpartners.api.endpoint.rest.model.PageSection restSection) {
    SectionPriority priority = SectionPriority.valueOf(restSection.getPriority().name());
    if (restSection instanceof app.bpartners.api.endpoint.rest.model.TextSection textRestSection) {
      return app.bpartners.api.service.annotation.model.custompage.TextSection.builder()
          .priority(priority)
          .text(textRestSection.getText())
          .build();
    } else if (restSection
        instanceof app.bpartners.api.endpoint.rest.model.ImageSection imageRestSection) {
      return app.bpartners.api.service.annotation.model.custompage.ImageSection.builder()
          .priority(priority)
          .url(resolveImageSectionUri(imageRestSection))
          .caption(imageRestSection.getCaption())
          .build();
    } else if (restSection
        instanceof app.bpartners.api.endpoint.rest.model.TableSection tableRestSection) {
      app.bpartners.api.endpoint.rest.model.TableData restTableData =
          tableRestSection.getTableData();
      return app.bpartners.api.service.annotation.model.custompage.TableSection.builder()
          .priority(priority)
          .tableData(
              TableData.builder()
                  .headers(restTableData.getHeaders())
                  .rows(restTableData.getRows())
                  .build())
          .build();
    } else if (restSection
        instanceof app.bpartners.api.endpoint.rest.model.SplitSection splitRestSection) {
      return split(priority, splitRestSection.getLeftSection(), splitRestSection.getRightSection());
    } else if (restSection
        instanceof app.bpartners.api.endpoint.rest.model.ThreeSplitSection threeSplitRestSection) {
      return split(
          priority,
          threeSplitRestSection.getLeftSection(),
          threeSplitRestSection.getMiddleSection(),
          threeSplitRestSection.getRightSection());
    }
    throw new IllegalArgumentException("Unknown section type: " + restSection.getClass());
  }

  private PageSection split(
      SectionPriority priority, app.bpartners.api.endpoint.rest.model.PageSection... columns) {
    return new app.bpartners.api.service.annotation.model.custompage.SplitSection(
        priority, Arrays.stream(columns).map(this::mapSection).toList());
  }

  private String resolveImageSectionUri(
      app.bpartners.api.endpoint.rest.model.ImageSection imageRestSection) {
    String fileId = imageRestSection.getFileId();
    if (fileId != null && !fileId.isBlank()) {
      var fromFile = imageUriFromFile(fileId);
      if (fromFile != null) {
        return fromFile;
      }
    }

    String imageUri = String.valueOf(imageRestSection.getUrl());
    var fromUrl = imageUriFromUrl(imageUri);
    return fromUrl != null ? fromUrl : imageUri;
  }

  private String imageUriFromFile(String fileId) {
    try {
      var fileInfo = fileService.findById(fileId);
      if (fileInfo == null) {
        log.warn("Can't get image file for file id: {}", fileId);
        return null;
      }
      var file =
          fileService.downloadFile(FileType.IMAGE, fileInfo.getUserUploaderId(), fileInfo.getId());
      if (file == null) {
        log.warn("Can't download image file for file id: {}", fileId);
        return null;
      }
      var image = ImageIO.read(file);
      if (image == null) {
        log.warn("Could not read image from file id: {}", fileId);
        return null;
      }
      return ImageUriUtils.bufferedImageToUri(image);
    } catch (IOException e) {
      log.error("Could not download image from file id: {}", fileId, e);
      return null;
    }
  }

  private static String imageUriFromUrl(String imageUri) {
    try {
      URI uri = new URI(imageUri);
      String scheme = uri.getScheme();
      if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
        log.warn("Blocked non-http/https image url: {}", imageUri);
        return null;
      }
      var image = ImageIO.read(uri.toURL());
      if (image == null) {
        log.warn("Could not read image from url: {}", imageUri);
        return null;
      }
      return ImageUriUtils.bufferedImageToUri(image);
    } catch (IOException | URISyntaxException | IllegalArgumentException e) {
      log.error("Could not download image from url: {}", imageUri, e);
      return null;
    }
  }
}
