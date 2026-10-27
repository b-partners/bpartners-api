package app.bpartners.api.service.annotation.factory;

import static app.bpartners.api.service.annotation.utils.ImageUriUtils.bufferedImageToUri;

import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.ImageCompressor;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotation3D;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.service.file.FileService;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Resolves the picture shown for each 3D pan and facade (uploaded image or top view). */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItemImagesResolver {
  private final FileService fileService;
  private final ExportAreaPictureAnnotationImage3DGenerator image3DGenerator;
  private final ImageCompressor imageCompressor;

  public List<String> pans(ExportAreaPictureAnnotation3D annotation3D) {
    return imagesOf(annotation3D.getPans(), "pan");
  }

  public List<String> facades(ExportAreaPictureAnnotation3D annotation3D) {
    if (annotation3D.getFacades() == null) {
      return List.of();
    }
    return imagesOf(annotation3D.getFacades(), "facade");
  }

  // Pans and facades share the same rendering: a highlighted top view, replaced by the item's own
  // uploaded image when there is one.
  private List<String> imagesOf(List<ExportAreaPictureAnnotation3DPan> items, String itemLabel) {
    var overallTopView = image3DGenerator.generateBaseImage(items);
    return items.stream()
        .map(
            item -> {
              var image =
                  image3DGenerator.generateBaseImageWithHighlightedPanWithSlopeBoundary(
                      overallTopView.second(), overallTopView.first(), item);
              try {
                if (item.getImageUri() == null || item.getImageUri().isBlank()) {
                  log.warn(
                      "No image provided for {}: {}. Falling back to top view image.",
                      itemLabel,
                      item.getName());
                  return bufferedImageToUri(image);
                }
                var fileInfo = fileService.findById(item.getImageUri());
                if (fileInfo == null) {
                  log.warn(
                      "Can't get image file for {}: {} from file id {}. Falling back to top view"
                          + " image.",
                      itemLabel,
                      item.getName(),
                      item.getImageUri());
                  return bufferedImageToUri(image);
                }
                var fileFromFileService =
                    fileService.downloadFile(
                        FileType.IMAGE, fileInfo.getUserUploaderId(), fileInfo.getId());

                if (fileFromFileService == null) {
                  log.warn(
                      "Can't get image file for {}: {}. Falling back to top view image.",
                      itemLabel,
                      item.getName());
                  return bufferedImageToUri(image);
                }

                image = ImageIO.read(fileFromFileService);
              } catch (IOException e) {
                log.error(
                    "Error while downloading {} image: {}. Falling back to top view image.",
                    itemLabel,
                    item.getName(),
                    e);
              }

              image = imageCompressor.compressImage(image);
              return bufferedImageToUri(image);
            })
        .toList();
  }
}
