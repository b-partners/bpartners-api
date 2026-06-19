package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.endpoint.rest.model.Point;
import app.bpartners.api.endpoint.rest.model.Polygon;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.model.Coordinates;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExportAreaPictureAnnotationImage3DGeneratorTest {

  private final ExportAreaPictureAnnotationImage3DGenerator subject =
      new ExportAreaPictureAnnotationImage3DGenerator();

  @Test
  void generateBaseImage_should_draw_pans_in_red() {
    var pan =
        new ExportAreaPictureAnnotation3DPan()
            .polygon(
                new Polygon()
                    .points(
                        List.of(
                            new Point().x(100d).y(100d),
                            new Point().x(200d).y(100d),
                            new Point().x(200d).y(200d),
                            new Point().x(100d).y(100d))));

    var result = subject.generateBaseImage(List.of(pan));
    BufferedImage image = result.second();

    var transform = result.first();
    var center = transform.apply(new Coordinates(new int[] {150}, new int[] {125}));

    assertColorEquals(Color.RED, image.getRGB(center.allX()[0], center.allY()[0]));
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
