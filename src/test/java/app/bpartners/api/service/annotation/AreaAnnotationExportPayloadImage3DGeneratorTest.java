package app.bpartners.api.service.annotation;

import app.bpartners.api.endpoint.rest.mapper.ExportAreaPictureAnnotationRestMapper;
import app.bpartners.api.service.annotation.export.AreaAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.model.RoofSlopeBoundaryType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import javax.imageio.ImageIO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Slf4j
class AreaAnnotationExportPayloadImage3DGeneratorTest {

  ObjectMapper objectMapper = new ObjectMapper();
  AreaAnnotationImage3DGenerator subject = new AreaAnnotationImage3DGenerator();
  ExportAreaPictureAnnotationRestMapper mapper = new ExportAreaPictureAnnotationRestMapper();

  @Test
  @SneakyThrows
  void base_image_with_edges_types() {
    AreaAnnotationExportPayload annotation = heavyAnnotationFromPayload();

    var actual =
        subject.generateBaseImageWithSlopeBoundariesWithMeasurement(
            annotation.getAnnotation3d().getPans());
    saveAndShow(actual.second());
  }

  @Test
  @SneakyThrows
  void base_image_with_areas() {
    AreaAnnotationExportPayload annotation = heavyAnnotationFromPayload();

    var actual = subject.generateBaseImageWithAreas(annotation.getAnnotation3d().getPans());
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

  private AreaAnnotationExportPayload heavyAnnotationFromPayload() throws IOException {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    var restAnnotation =
        objectMapper.readValue(
            new ClassPathResource("payload/heavy-export-pdf-payload.json").getInputStream(),
            app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation.class);

    var domainAnnotation = mapper.toDomain(restAnnotation);

    var updated3d = domainAnnotation.getAnnotation3d();
    if (updated3d != null) {
      var updatedPans = new ArrayList<AreaAnnotation3DPan>();
      for (var pan : updated3d.getPans()) {
        int lines = pan.getPolygon().points().size() - 1;
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
        String jsonEdgeTypes;
        try {
          jsonEdgeTypes = objectMapper.writeValueAsString(edgeTypes);
        } catch (JsonProcessingException e) {
          throw new RuntimeException(e);
        }
        var updatedInfos = new ArrayList<>(pan.getInfos());
        updatedInfos.add(new AreaAnnotationInstanceInfo("edgeTypes", jsonEdgeTypes));
        updatedPans.add(pan.toBuilder().imageUri("imageUri").infos(updatedInfos).build());
      }
      domainAnnotation =
          domainAnnotation.toBuilder()
              .annotation3d(updated3d.toBuilder().pans(updatedPans).build())
              .build();
    }

    return domainAnnotation;
  }
}
