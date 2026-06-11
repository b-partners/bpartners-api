package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.model.Coordinates;
import app.bpartners.api.service.annotation.model.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.service.annotation.model.ExportAreaPictureAnnotationInstanceInfo;
import app.bpartners.api.service.annotation.model.Point;
import app.bpartners.api.service.annotation.model.Polygon;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExportAreaPictureAnnotationImage3DGeneratorTest {

  private final ExportAreaPictureAnnotationImage3DGenerator subject =
      new ExportAreaPictureAnnotationImage3DGenerator();

  @Test
  void generateBaseImageWithSlopeBoundary_should_draw_correct_colors() {
    var pan = new ExportAreaPictureAnnotation3DPan();
    var polygon = new Polygon();
    polygon.setPoints(
        List.of(
            new Point().setX(100d).setY(100d),
            new Point().setX(200d).setY(100d),
            new Point().setX(200d).setY(200d),
            new Point().setX(100d).setY(200d),
            new Point().setX(100d).setY(100d)));
    pan.setPolygon(polygon);
    var info = new ExportAreaPictureAnnotationInstanceInfo();
    info.setLabel("edgeTypes");
    info.setValue("[\"faitage\", \"egout\", \"rive\", \"noue\"]");
    pan.setInfos(List.of(info));

    var result = subject.generateBaseImageWithSlopeBoundary(List.of(pan));
    BufferedImage image = result.second();

    assertNotNull(image);
    assertEquals(550, image.getWidth());
    assertEquals(550, image.getHeight());

    var transform = result.first();
    var coords =
        transform.apply(
            new Coordinates(new int[] {150, 200, 150, 100}, new int[] {100, 150, 200, 150}));

    assertColorEquals(new Color(220, 20, 60), image.getRGB(coords.allX()[0], coords.allY()[0]));
    // egout color: 30, 144, 255
    assertColorEquals(new Color(30, 144, 255), image.getRGB(coords.allX()[1], coords.allY()[1]));
    // rive color: 34, 139, 34
    assertColorEquals(new Color(34, 139, 34), image.getRGB(coords.allX()[2], coords.allY()[2]));
    // noue color: 148, 0, 211
    assertColorEquals(new Color(148, 0, 211), image.getRGB(coords.allX()[3], coords.allY()[3]));
  }

  @Test
  void generateBaseImage_should_draw_pans_in_red() {
    var pan = new ExportAreaPictureAnnotation3DPan();
    var polygon = new Polygon();
    polygon.setPoints(
        List.of(
            new Point().setX(100d).setY(100d),
            new Point().setX(200d).setY(100d),
            new Point().setX(200d).setY(200d),
            new Point().setX(100d).setY(100d)));
    pan.setPolygon(polygon);

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
