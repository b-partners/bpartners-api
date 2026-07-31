package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.service.annotation.AreaAnnotation3DPan;
import app.bpartners.api.service.annotation.Point;
import app.bpartners.api.service.annotation.Polygon;
import app.bpartners.api.service.annotation.export.AreaAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.model.Coordinates;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.Test;

class AreaAnnotationExportPayloadImage3DGeneratorTest {

  private final AreaAnnotationImage3DGenerator subject = new AreaAnnotationImage3DGenerator();

  @Test
  void generateBaseImage_should_draw_pans_in_red() {
    var pan =
        AreaAnnotation3DPan.builder()
            .polygon(
                new Polygon(
                    List.of(
                        new Point(100, 100),
                        new Point(200, 100),
                        new Point(200, 200),
                        new Point(100, 100))))
            .build();

    var result = subject.generateBaseImage(List.of(pan));
    BufferedImage image = result.second();

    var transform = result.first();
    var center = transform.apply(new Coordinates(new int[] {150}, new int[] {125}));

    assertColorEquals(Color.RED, image.getRGB(center.allX()[0], center.allY()[0]));
  }

  @Test
  void summary_image_should_not_be_clipped_on_the_right_edge() {
    var widePan =
        AreaAnnotation3DPan.builder()
            .polygon(
                new Polygon(
                    List.of(
                        new Point(0, 0),
                        new Point(200, 0),
                        new Point(200, 50),
                        new Point(0, 50),
                        new Point(0, 0))))
            .measurements(List.of())
            .infos(List.of())
            .build();

    var result = subject.generateBaseImageWithSlopeBoundariesWithMeasurement(List.of(widePan));
    BufferedImage image = result.second();

    // The polygon (and its stroke) must not reach the image edge: the rightmost columns
    // must stay transparent (no drawn pixel). Regression for the clipped base image bug.
    for (int x = image.getWidth() - 5; x < image.getWidth(); x++) {
      for (int y = 0; y < image.getHeight(); y++) {
        int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
        assertEquals(0, alpha, "Pixel at (" + x + "," + y + ") should be transparent");
      }
    }
  }

  @Test
  void summary_image_should_not_be_clipped_on_the_bottom_edge() {
    var tallPan =
        AreaAnnotation3DPan.builder()
            .polygon(
                new Polygon(
                    List.of(
                        new Point(0, 0),
                        new Point(50, 0),
                        new Point(50, 200),
                        new Point(0, 200),
                        new Point(0, 0))))
            .measurements(List.of())
            .infos(List.of())
            .build();

    var result = subject.generateBaseImageWithSlopeBoundariesWithMeasurement(List.of(tallPan));
    BufferedImage image = result.second();

    // Height-bound pans must not be clipped at the bottom either: the last rows
    // must stay transparent (no drawn pixel).
    for (int y = image.getHeight() - 5; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        int alpha = (image.getRGB(x, y) >>> 24) & 0xFF;
        assertEquals(0, alpha, "Pixel at (" + x + "," + y + ") should be transparent");
      }
    }
  }

  @Test
  void mergePanImagesSideBySide_should_work() {
    BufferedImage img1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
    BufferedImage img2 = new BufferedImage(200, 150, BufferedImage.TYPE_INT_RGB);

    BufferedImage result = subject.mergePanImagesSideBySide(img1, img2);

    assertEquals(300, result.getWidth());
    assertEquals(150, result.getHeight());
  }

  private void assertColorEquals(Color expected, int actualRgb) {
    Color actual = new Color(actualRgb, true);
    assertEquals(expected.getRed(), actual.getRed(), "Red channel mismatch");
    assertEquals(expected.getGreen(), actual.getGreen(), "Green channel mismatch");
    assertEquals(expected.getBlue(), actual.getBlue(), "Blue channel mismatch");
  }
}
