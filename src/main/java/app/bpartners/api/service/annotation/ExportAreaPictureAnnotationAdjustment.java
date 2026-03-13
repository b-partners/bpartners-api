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
          if (pan.getPolygon() != null && pan.getPolygon().getPoints() != null) {
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
    if (originalImage == null
        || compressedImage == null
        || exportAnnotation.getAnnotations() == null) {
      return new RescaleValue(1.0, 1.0);
    }

    double scaleX = (double) compressedImage.getWidth() / originalImage.getWidth();
    double scaleY = (double) compressedImage.getHeight() / originalImage.getHeight();

    for (var annotation : exportAnnotation.getAnnotations()) {
      if (annotation.getPolygon() != null && annotation.getPolygon().getPoints() != null) {
        for (var point : annotation.getPolygon().getPoints()) {
          point.setX(point.getX() * scaleX);
          point.setY(point.getY() * scaleY);
        }
      }
    }

    return new RescaleValue(scaleX, scaleY);
  }

  record RescaleValue(double x, double y) {}
}
