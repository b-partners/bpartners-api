package app.bpartners.api.unit.service.annotation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.service.annotation.factory.Graphics2DFactory;
import app.bpartners.api.service.annotation.model.Coordinates;
import app.bpartners.api.service.annotation.model.Drawer;
import app.bpartners.api.service.annotation.model.RawCoordinates;
import app.bpartners.api.service.annotation.model.Transform;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class DrawerTest {
  private static final int IMAGE_SIZE = 500;
  private static final String OUTPUT_DIR = "build/drawer-test";

  private static List<ExportAreaPictureAnnotation3DPan> roofPanels;

  @BeforeAll
  static void setup() throws IOException {
    var objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    ExportAreaPictureAnnotation annotation =
        objectMapper.readValue(
            new ClassPathResource("payload/export/1 rue de la vau saint-jacques.json")
                .getInputStream(),
            ExportAreaPictureAnnotation.class);

    roofPanels = annotation.get3d().getPans();
  }

  @Test
  void payload_should_expose_the_expected_roof_panel_polygons() {
    assertEquals(5, roofPanels.size());
    assertEquals(
        List.of("Pan A", "Pan B", "Pan C", "Pan D", "Polygone 1"),
        roofPanels.stream().map(ExportAreaPictureAnnotation3DPan::getName).toList());
  }

  @Test
  void drawFillPolygon_should_fill_every_extracted_roof_panel_polygon() throws IOException {
    for (ExportAreaPictureAnnotation3DPan pan : roofPanels) {
      var mapped = mapToImageSpace(RawCoordinates.from(pan.getPolygon()));
      var image = newImage();
      var g2d = newGraphics(image);

      Drawer.drawFillPolygon(g2d, Color.RED, mapped);
      g2d.dispose();

      var interior = interiorPointOf(mapped);
      assertColorEquals(
          Color.RED, image.getRGB(interior[0], interior[1]), "fill color of " + pan.getName());
      savePng(image, "fill-" + pan.getName());
    }
  }

  @Test
  void drawStrokePolygon_should_draw_the_border_of_every_extracted_roof_panel_polygon()
      throws IOException {
    for (ExportAreaPictureAnnotation3DPan pan : roofPanels) {
      var mapped = mapToImageSpace(RawCoordinates.from(pan.getPolygon()));
      var image = newImage();
      var g2d = newGraphics(image);

      Drawer.drawStrokePolygon(g2d, Color.BLUE, new BasicStroke(1f), mapped);
      g2d.dispose();

      int firstVertexX = mapped.allX()[0];
      int firstVertexY = mapped.allY()[0];
      assertColorEquals(
          Color.BLUE,
          image.getRGB(firstVertexX, firstVertexY),
          "stroke color at first vertex of " + pan.getName());
      savePng(image, "stroke-" + pan.getName());
    }
  }

  @Test
  void drawPolygonPoints_should_mark_every_vertex_of_every_extracted_roof_panel_polygon()
      throws IOException {
    for (ExportAreaPictureAnnotation3DPan pan : roofPanels) {
      var mapped = mapToImageSpace(RawCoordinates.from(pan.getPolygon()));
      var image = newImage();
      var g2d = newGraphics(image);

      Drawer.drawPolygonPoints(g2d, Color.GREEN, 6, mapped);
      g2d.dispose();

      for (int i = 0; i < mapped.allX().length; i++) {
        assertColorEquals(
            Color.GREEN,
            image.getRGB(mapped.allX()[i], mapped.allY()[i]),
            "vertex point " + i + " of " + pan.getName());
      }
      savePng(image, "points-" + pan.getName());
    }
  }

  @Test
  void draw_all_roof_panels_together_should_produce_a_visible_composite_image() throws IOException {
    var image = newImage();
    var g2d = Graphics2DFactory.make(image);

    var transform = combinedTransform(roofPanels);
    var colors =
        List.of(Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN, Color.MAGENTA);

    for (int i = 0; i < roofPanels.size(); i++) {
      var pan = roofPanels.get(i);
      var mapped = transform.apply(RawCoordinates.from(pan.getPolygon()));
      var fillColor = colors.get(i % colors.size());

      Drawer.drawFillPolygon(
          g2d,
          new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 120),
          mapped);
      Drawer.drawStrokePolygon(g2d, Color.BLACK, new BasicStroke(2f), mapped);
      Drawer.drawPolygonPoints(g2d, Color.BLACK, 6, mapped);
    }

    g2d.dispose();

    savePng(image, "all-roof-panels");
  }

  private static Coordinates mapToImageSpace(RawCoordinates polygon) {
    var transform = Transform.from(polygon, IMAGE_SIZE - 10, IMAGE_SIZE);
    return transform.apply(polygon);
  }

  private static Transform combinedTransform(List<ExportAreaPictureAnnotation3DPan> pans) {
    var allX =
        pans.stream()
            .flatMapToDouble(pan -> Arrays.stream(RawCoordinates.from(pan.getPolygon()).allX()))
            .toArray();
    var allY =
        pans.stream()
            .flatMapToDouble(pan -> Arrays.stream(RawCoordinates.from(pan.getPolygon()).allY()))
            .toArray();

    return Transform.from(new RawCoordinates(allX, allY), IMAGE_SIZE - 10, IMAGE_SIZE);
  }

  private static void savePng(BufferedImage image, String name) throws IOException {
    Files.createDirectories(Paths.get(OUTPUT_DIR));
    ImageIO.write(image, "png", new File(OUTPUT_DIR + "/" + name + ".png"));
  }

  private static BufferedImage newImage() {
    return new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_ARGB);
  }

  private static Graphics2D newGraphics(BufferedImage image) {
    return image.createGraphics();
  }

  /** Reproduces {@code Drawer#polygonCentroid}, verified to fall inside every extracted pan. */
  private static int[] interiorPointOf(Coordinates polygon) {
    double area = 0;
    double cx = 0;
    double cy = 0;
    int n = polygon.allX().length;

    for (int i = 0; i < n; i++) {
      int j = (i + 1) % n;
      double cross =
          (double) polygon.allX()[i] * polygon.allY()[j]
              - (double) polygon.allX()[j] * polygon.allY()[i];
      area += cross;
      cx += (polygon.allX()[i] + polygon.allX()[j]) * cross;
      cy += (polygon.allY()[i] + polygon.allY()[j]) * cross;
    }

    area *= 0.5;
    cx /= (6 * area);
    cy /= (6 * area);

    return new int[] {(int) Math.round(cx), (int) Math.round(cy)};
  }

  private static void assertColorEquals(Color expected, int actualRgb, String what) {
    Color actual = new Color(actualRgb, true);
    assertTrue(
        expected.getRed() == actual.getRed()
            && expected.getGreen() == actual.getGreen()
            && expected.getBlue() == actual.getBlue()
            && actual.getAlpha() == 255,
        what + " expected " + expected + " but was " + actual);
  }
}
