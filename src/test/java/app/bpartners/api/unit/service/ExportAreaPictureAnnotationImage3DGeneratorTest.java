package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationMeasurement;
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
  void generateBaseImage_should_not_invert_the_y_axis() {
    // The input coordinates are world coordinates where Y increases upward
    // (e.g. northing for pans), while the image Y axis increases downward.
    // A source point at the top of the polygon (min Y) must be drawn at the
    // bottom of the image, and a source point at the bottom (max Y) at the top.
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
    var transform = result.first();

    var sourceTop = transform.apply(new Coordinates(new int[] {150}, new int[] {100}));
    var sourceBottom = transform.apply(new Coordinates(new int[] {150}, new int[] {200}));

    assertTrue(
        sourceTop.allY()[0] > sourceBottom.allY()[0],
        "Y axis should be flipped: source top (min Y) must be drawn below source bottom (max Y)");
  }

  @Test
  void generatePanImageWithMeasurements_should_draw_pan_without_inverting_the_y_axis() {
    // The oriented polygon coordinates sent by the frontend are already expressed
    // in screen space (Y increases downward, matching the captured pan images), so
    // the pan image must NOT invert the Y axis: the north apex (max Y, x=120) must
    // be drawn near the BOTTOM of the image, not at the top.
    var pan =
        new ExportAreaPictureAnnotation3DPan()
            .polygon(
                new Polygon()
                    .points(
                        List.of(
                            new Point().x(100d).y(100d),
                            new Point().x(300d).y(100d),
                            new Point().x(300d).y(300d),
                            new Point().x(120d).y(300d),
                            new Point().x(100d).y(100d))))
            .measurements(
                List.of(
                    invisibleMeasurement(),
                    invisibleMeasurement(),
                    invisibleMeasurement(),
                    invisibleMeasurement()));

    var image = subject.generatePanImageWithMeasurements(pan);

    // North apex (120, 300) maps to (79, 520): a black point must be drawn there.
    assertNearBlack(
        new Color(image.getRGB(79, 520), true), "north apex point (should be near the bottom)");
    // No point sits at (79, 30): only the south edge stroke crosses there.
    assertNotNearBlack(
        new Color(image.getRGB(79, 30), true), "south edge midpoint (no point expected)");
  }

  @Test
  void mergePanImagesSideBySide_should_work() {
    BufferedImage img1 = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
    BufferedImage img2 = new BufferedImage(200, 150, BufferedImage.TYPE_INT_RGB);

    BufferedImage result = subject.mergePanImagesSideBySide(img1, img2);

    assertEquals(300, result.getWidth());
    assertEquals(150, result.getHeight());
  }

  private static ExportAreaPictureAnnotationMeasurement invisibleMeasurement() {
    return new ExportAreaPictureAnnotationMeasurement().value(2.0).unit("m").isInvisible(true);
  }

  private static void assertNearBlack(Color color, String what) {
    assertTrue(
        color.getRed() < 25 && color.getGreen() < 25 && color.getBlue() < 25,
        what + " should be near black but was " + color);
  }

  private static void assertNotNearBlack(Color color, String what) {
    assertTrue(
        color.getRed() >= 25 || color.getGreen() >= 25 || color.getBlue() >= 25,
        what + " should not be near black but was " + color);
  }

  private void assertColorEquals(Color expected, int actualRgb) {
    Color actual = new Color(actualRgb, true);
    assertEquals(expected.getRed(), actual.getRed(), "Red channel mismatch");
    assertEquals(expected.getGreen(), actual.getGreen(), "Green channel mismatch");
    assertEquals(expected.getBlue(), actual.getBlue(), "Blue channel mismatch");
  }
}
