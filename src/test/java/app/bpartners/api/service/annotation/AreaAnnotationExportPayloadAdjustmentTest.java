package app.bpartners.api.service.annotation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.service.annotation.export.AreaAnnotationAdjustment;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class AreaAnnotationExportPayloadAdjustmentTest {

  @Test
  void adjust3DAnnotation_ok() throws IOException {
    var pan =
        AreaAnnotation3DPan.builder()
            .name("pan1")
            .polygon(
                new Polygon(
                    List.of(
                        new Point(100.0, 200.0), new Point(200.0, 100.0), new Point(300.0, 300.0))))
            .measurements(List.of())
            .infos(List.of())
            .build();
    var annotation3D = AreaAnnotation3D.builder().pans(List.of(pan)).build();
    var exportAnnotation =
        AreaAnnotationExportPayload.builder()
            .address("addr")
            .imageUrl("url")
            .annotation3d(annotation3D)
            .annotations(List.of())
            .build();
    BufferedImage originalImg = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
    BufferedImage compressedImg = new BufferedImage(500, 250, BufferedImage.TYPE_INT_RGB);
    byte[] originalBytes = toByteArray(originalImg);
    byte[] compressedBytes = toByteArray(compressedImg);

    var adjusted =
        AreaAnnotationAdjustment.adjust3DAnnotation(
            exportAnnotation, originalBytes, compressedBytes);

    var adjustedPan = adjusted.getAnnotation3d().getPans().getFirst();
    var adjustedPoint = adjustedPan.getPolygon().points().getFirst();
    assertEquals(50.0, adjustedPoint.x());
    assertEquals(50.0, adjustedPoint.y());
  }

  @Test
  void adjust3DAnnotation_null_params() {
    assertDoesNotThrow(() -> AreaAnnotationAdjustment.adjust3DAnnotation(null, null, null));
  }

  @Test
  void adjustAnnotation_ok() {
    var annotationInstance =
        AreaAnnotationInstance.builder()
            .polygon(
                new Polygon(
                    List.of(
                        new Point(100.0, 200.0), new Point(200.0, 100.0), new Point(300.0, 300.0))))
            .infos(List.of(new AreaAnnotationInstanceInfo("key", "test")))
            .measurements(List.of())
            .build();
    var exportAnnotation =
        AreaAnnotationExportPayload.builder()
            .address("addr")
            .imageUrl("url")
            .annotations(List.of(annotationInstance))
            .build();
    BufferedImage originalImg = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
    BufferedImage compressedImg = new BufferedImage(500, 250, BufferedImage.TYPE_INT_RGB);

    var result =
        AreaAnnotationAdjustment.adjustAnnotation(exportAnnotation, originalImg, compressedImg);

    assertEquals(0.5, result.rescaleValue().x());
    assertEquals(0.25, result.rescaleValue().y());
    var adjustedAnnotation = result.adjustedAnnotation();
    var adjustedPoint =
        adjustedAnnotation.getAnnotations().getFirst().getPolygon().points().getFirst();
    assertEquals(50.0, adjustedPoint.x());
    assertEquals(50.0, adjustedPoint.y());
  }

  @Test
  void adjustAnnotation_null_params() {
    var result = AreaAnnotationAdjustment.adjustAnnotation(null, null, null);
    assertEquals(1.0, result.rescaleValue().x());
    assertEquals(1.0, result.rescaleValue().y());
  }

  @Test
  void adjust3DAnnotation_io_exception() {
    var exportAnnotation =
        AreaAnnotationExportPayload.builder()
            .address("addr")
            .imageUrl("url")
            .annotations(List.of())
            .build();
    byte[] invalidBytes = new byte[] {0, 0, 0};

    // Should not throw, just log a warning
    assertDoesNotThrow(
        () ->
            AreaAnnotationAdjustment.adjust3DAnnotation(
                exportAnnotation, invalidBytes, invalidBytes));
  }

  private byte[] toByteArray(BufferedImage img) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(img, "png", baos);
    return baos.toByteArray();
  }
}
