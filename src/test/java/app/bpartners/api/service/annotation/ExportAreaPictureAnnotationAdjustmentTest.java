package app.bpartners.api.service.annotation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.service.annotation.model.ExportAreaPictureAnnotation;
import app.bpartners.api.service.annotation.model.ExportAreaPictureAnnotation3D;
import app.bpartners.api.service.annotation.model.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.service.annotation.model.ExportAreaPictureAnnotationInstance;
import app.bpartners.api.service.annotation.model.Point;
import app.bpartners.api.service.annotation.model.Polygon;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ExportAreaPictureAnnotationAdjustmentTest {

  @Test
  void adjust3DAnnotation_ok() throws IOException {
    ExportAreaPictureAnnotation exportAnnotation = mock(ExportAreaPictureAnnotation.class);
    ExportAreaPictureAnnotation3D annotation3D = mock(ExportAreaPictureAnnotation3D.class);
    ExportAreaPictureAnnotation3DPan pan = mock(ExportAreaPictureAnnotation3DPan.class);
    Polygon polygon = mock(Polygon.class);
    Point point = new Point();
    point.setX(100.0);
    point.setY(200.0);
    when(exportAnnotation.get3d()).thenReturn(annotation3D);
    when(annotation3D.getPans()).thenReturn(List.of(pan));
    when(pan.getPolygon()).thenReturn(polygon);
    when(polygon.getPoints()).thenReturn(List.of(point));
    BufferedImage originalImg = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
    BufferedImage compressedImg = new BufferedImage(500, 250, BufferedImage.TYPE_INT_RGB);
    byte[] originalBytes = toByteArray(originalImg);
    byte[] compressedBytes = toByteArray(compressedImg);

    ExportAreaPictureAnnotationAdjustment.adjust3DAnnotation(
        exportAnnotation, originalBytes, compressedBytes);

    assertEquals(50.0, point.getX());
    assertEquals(50.0, point.getY());
  }

  @Test
  void adjust3DAnnotation_null_params() {
    assertDoesNotThrow(
        () -> ExportAreaPictureAnnotationAdjustment.adjust3DAnnotation(null, null, null));
  }

  @Test
  void adjustAnnotation_ok() {
    ExportAreaPictureAnnotation exportAnnotation = mock(ExportAreaPictureAnnotation.class);
    ExportAreaPictureAnnotationInstance annotation =
        mock(ExportAreaPictureAnnotationInstance.class);
    Polygon polygon = mock(Polygon.class);
    Point point = new Point();
    point.setX(100.0);
    point.setY(200.0);
    when(exportAnnotation.getAnnotations()).thenReturn(List.of(annotation));
    when(annotation.getPolygon()).thenReturn(polygon);
    when(polygon.getPoints()).thenReturn(List.of(point));
    BufferedImage originalImg = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
    BufferedImage compressedImg = new BufferedImage(500, 250, BufferedImage.TYPE_INT_RGB);

    ExportAreaPictureAnnotationAdjustment.RescaleValue actualRescale =
        ExportAreaPictureAnnotationAdjustment.adjustAnnotation(
            exportAnnotation, originalImg, compressedImg);

    assertEquals(0.5, actualRescale.x());
    assertEquals(0.25, actualRescale.y());
    assertEquals(50.0, point.getX());
    assertEquals(50.0, point.getY());
  }

  @Test
  void adjustAnnotation_null_params() {
    var rescale = ExportAreaPictureAnnotationAdjustment.adjustAnnotation(null, null, null);
    assertEquals(1.0, rescale.x());
    assertEquals(1.0, rescale.y());
  }

  @Test
  void adjust3DAnnotation_io_exception() {
    ExportAreaPictureAnnotation exportAnnotation = mock(ExportAreaPictureAnnotation.class);
    byte[] invalidBytes = new byte[] {0, 0, 0};

    // Should not throw, just log a warning
    assertDoesNotThrow(
        () ->
            ExportAreaPictureAnnotationAdjustment.adjust3DAnnotation(
                exportAnnotation, invalidBytes, invalidBytes));
  }

  private byte[] toByteArray(BufferedImage img) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(img, "png", baos);
    return baos.toByteArray();
  }
}
