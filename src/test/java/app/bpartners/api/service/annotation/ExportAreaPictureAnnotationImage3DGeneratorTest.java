package app.bpartners.api.service.annotation;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstanceInfo;
import app.bpartners.api.service.annotation.model.RoofSlopeBoundaryType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import javax.imageio.ImageIO;

@Slf4j
class ExportAreaPictureAnnotationImage3DGeneratorTest {

  ObjectMapper objectMapper = new ObjectMapper();
  ExportAreaPictureAnnotationImage3DGenerator subject =
      new ExportAreaPictureAnnotationImage3DGenerator();

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

  private ExportAreaPictureAnnotation annotationFromPayload() throws IOException {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return objectMapper.readValue(
        new ClassPathResource("payload/export-pdf-payload.json").getInputStream(),
        ExportAreaPictureAnnotation.class);
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
}
