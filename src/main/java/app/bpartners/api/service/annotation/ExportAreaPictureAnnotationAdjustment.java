package app.bpartners.api.service.annotation;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExportAreaPictureAnnotationAdjustment {

  public static void adjust3DAnnotation(
      ExportAreaPictureAnnotation exportAnnotation,
      byte[] originalImage3D,
      byte[] compressedImage3D) {
    if (originalImage3D == null || compressedImage3D == null || exportAnnotation.get3d() == null) {
      return;
    }

    try {
      BufferedImage original = ImageIO.read(new ByteArrayInputStream(originalImage3D));
      BufferedImage compressed = ImageIO.read(new ByteArrayInputStream(compressedImage3D));
      if (original != null && compressed != null) {
        double scaleX = (double) compressed.getWidth() / original.getWidth();
        double scaleY = (double) compressed.getHeight() / original.getHeight();

        for (var pan : exportAnnotation.get3d().getPans()) {
          if (pan.getPolygon().getPoints() != null) {
            for (var point : pan.getPolygon().getPoints()) {
              point.setX(point.getX() * scaleX);
              point.setY(point.getY() * scaleY);
            }
          }
        }
      }
    } catch (IOException e) {
      log.warn("Could not adjust 3D annotations", e);
    }
  }

  public static RescaleValue adjustAnnotation(
      ExportAreaPictureAnnotation exportAnnotation,
      BufferedImage originalImage,
      BufferedImage compressedImage) {
    if (originalImage == null || compressedImage == null) {
      return new RescaleValue(1.0, 1.0);
    }
    return adjustAnnotation(
        exportAnnotation, originalImage.getWidth(), originalImage.getHeight(), compressedImage);
  }

  /**
   * Same as {@link #adjustAnnotation(ExportAreaPictureAnnotation, BufferedImage, BufferedImage)}
   * but takes the source image's true dimensions directly, so callers that decode a
   * memory-bounded/subsampled version of a very large source photo can still rescale annotation
   * coordinates against the real, full-resolution size the frontend annotated against.
   */
  public static RescaleValue adjustAnnotation(
      ExportAreaPictureAnnotation exportAnnotation,
      int originalWidth,
      int originalHeight,
      BufferedImage compressedImage) {
    if (compressedImage == null) {
      return new RescaleValue(1.0, 1.0);
    }

    double scaleX = (double) compressedImage.getWidth() / originalWidth;
    double scaleY = (double) compressedImage.getHeight() / originalHeight;

    for (var annotation : exportAnnotation.getAnnotations()) {
      if (annotation.getPolygon().getPoints() != null) {
        for (var point : annotation.getPolygon().getPoints()) {
          point.setX(point.getX() * scaleX);
          point.setY(point.getY() * scaleY);
        }
      }
    }

    return new RescaleValue(scaleX, scaleY);
  }

  public record RescaleValue(double x, double y) {}
}
