package app.bpartners.api.service.annotation.export;

import app.bpartners.api.service.annotation.AreaAnnotation3D;
import app.bpartners.api.service.annotation.AreaAnnotationExportPayload;
import app.bpartners.api.service.annotation.AreaAnnotationInstance;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AreaAnnotationAdjustment {

  public static AreaAnnotationExportPayload adjust3DAnnotation(
      AreaAnnotationExportPayload exportAnnotation,
      byte[] originalImage3D,
      byte[] compressedImage3D) {
    if (originalImage3D == null
        || compressedImage3D == null
        || exportAnnotation.getAnnotation3d() == null) {
      return exportAnnotation;
    }

    try {
      BufferedImage original = ImageIO.read(new ByteArrayInputStream(originalImage3D));
      BufferedImage compressed = ImageIO.read(new ByteArrayInputStream(compressedImage3D));
      if (original != null && compressed != null) {
        double scaleX = (double) compressed.getWidth() / original.getWidth();
        double scaleY = (double) compressed.getHeight() / original.getHeight();
        var updated3d = scale3D(exportAnnotation.getAnnotation3d(), scaleX, scaleY);
        return exportAnnotation.toBuilder().annotation3d(updated3d).build();
      }
    } catch (IOException e) {
      log.warn("Could not adjust 3D annotations", e);
    }
    return exportAnnotation;
  }

  private static AreaAnnotation3D scale3D(
      AreaAnnotation3D annotation3D, double scaleX, double scaleY) {
    return annotation3D.toBuilder()
        .pans(annotation3D.getPans().stream().map(pan -> pan.scale(scaleX, scaleY)).toList())
        .facades(annotation3D.getFacades().stream().map(pan -> pan.scale(scaleX, scaleY)).toList())
        .build();
  }

  public static AdjustedAnnotationResult adjustAnnotation(
      AreaAnnotationExportPayload exportAnnotation,
      BufferedImage originalImage,
      BufferedImage compressedImage) {
    if (originalImage == null || compressedImage == null) {
      return new AdjustedAnnotationResult(exportAnnotation, new RescaleValue(1.0, 1.0));
    }

    double scaleX = (double) compressedImage.getWidth() / originalImage.getWidth();
    double scaleY = (double) compressedImage.getHeight() / originalImage.getHeight();

    var scaledAnnotations =
        exportAnnotation.getAnnotations().stream()
            .map(inst -> scaleInstance(inst, scaleX, scaleY))
            .toList();

    var adjustedAnnotation = exportAnnotation.toBuilder().annotations(scaledAnnotations).build();
    return new AdjustedAnnotationResult(adjustedAnnotation, new RescaleValue(scaleX, scaleY));
  }

  private static AreaAnnotationInstance scaleInstance(
      AreaAnnotationInstance instance, double scaleX, double scaleY) {
    if (instance.getPolygon() == null) {
      return instance;
    }
    return instance.toBuilder().polygon(instance.getPolygon().scale(scaleX, scaleY)).build();
  }

  public record RescaleValue(double x, double y) {}

  public record AdjustedAnnotationResult(
      AreaAnnotationExportPayload adjustedAnnotation, RescaleValue rescaleValue) {}
}
