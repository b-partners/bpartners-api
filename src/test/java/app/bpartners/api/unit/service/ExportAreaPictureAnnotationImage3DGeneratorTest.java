package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstanceInfo;
import app.bpartners.api.endpoint.rest.model.Point;
import app.bpartners.api.endpoint.rest.model.Polygon;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.model.Coordinates;
import app.bpartners.api.service.annotation.model.RoofSlopeBoundaryType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Slf4j
class ExportAreaPictureAnnotationImage3DGeneratorTest {

  ObjectMapper objectMapper = new ObjectMapper();
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

  @Test
  @SneakyThrows
  void base_image_with_edges_types() {
    ExportAreaPictureAnnotation annotation = heavyAnnotationFromPayload();

    var actual =
        subject.generateBaseImageWithSlopeBoundariesWithMeasurement(annotation.get3d().getPans());
    saveAndShow(actual.second());
  }

  @Test
  @SneakyThrows
  void base_image_with_areas() {
    ExportAreaPictureAnnotation annotation = heavyAnnotationFromPayload();

    var actual = subject.generateBaseImageWithAreas(annotation.get3d().getPans());
    saveAndShow(actual);
  }

  private void saveAndShow(BufferedImage actual) throws IOException {
    var file = saveImage(actual);

    log.info("Image saved to {}", file.getAbsolutePath());
  }

  private File saveImage(BufferedImage image) throws IOException {
    File file = Files.createTempFile("image-", ".png").toFile();
    try {
      ImageIO.write(image, "png", file);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return file;
  }

  private ExportAreaPictureAnnotation heavyAnnotationFromPayload() throws IOException {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    var annotation =
        objectMapper.readValue(
            new ClassPathResource("payload/heavy-export-pdf-payload.json").getInputStream(),
            ExportAreaPictureAnnotation.class);

    annotation
        .get3d()
        .getPans()
        .forEach(
            pan -> {
              int lines = pan.getPolygon().getPoints().size() - 1;
              var edgeTypes = new String[lines];

              var possibleTypes =
                  Arrays.stream(RoofSlopeBoundaryType.values())
                      .map(t -> t.getLabel().toLowerCase())
                      .toList();
              for (int i = 0; i < lines; i++) {
                var randomType =
                    possibleTypes.get(new Random().nextInt(possibleTypes.size())).replace("_", "-");
                edgeTypes[i] = randomType;
              }

              String jsonEdgeTypes = null;
              try {
                jsonEdgeTypes = objectMapper.writeValueAsString(edgeTypes);
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
              pan.getInfos()
                  .add(
                      new ExportAreaPictureAnnotationInstanceInfo()
                          .label("edgeTypes")
                          .value(jsonEdgeTypes));
              pan.setImageUri("imageUri");
            });

    return annotation;
  }

  private void assertColorEquals(Color expected, int actualRgb) {
    Color actual = new Color(actualRgb, true);
    assertEquals(expected.getRed(), actual.getRed(), "Red channel mismatch");
    assertEquals(expected.getGreen(), actual.getGreen(), "Green channel mismatch");
    assertEquals(expected.getBlue(), actual.getBlue(), "Blue channel mismatch");
  }
}
