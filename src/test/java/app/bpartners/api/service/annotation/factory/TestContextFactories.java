package app.bpartners.api.service.annotation.factory;

import static org.mockito.Mockito.mock;

import app.bpartners.api.endpoint.rest.mapper.ExportAreaPictureAnnotationRestMapper;
import app.bpartners.api.endpoint.rest.mapper.detection.AreaPictureAnnotationConfRestMapper;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.ImageCompressor;
import app.bpartners.api.service.annotation.model.export.ExportAreaPictureAnnotation;
import app.bpartners.api.service.file.FileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;

public final class TestContextFactories {
  private TestContextFactories() {}

  public static ExportAnnotationContextFactory contextFactory(FileService fileService) {
    var image3DGenerator = new ExportAreaPictureAnnotationImage3DGenerator();
    return new ExportAnnotationContextFactory(
        itemImagesResolver(fileService, image3DGenerator), image3DGenerator);
  }

  public static ItemImagesResolver itemImagesResolver(
      FileService fileService, ExportAreaPictureAnnotationImage3DGenerator image3DGenerator) {
    return new ItemImagesResolver(fileService, image3DGenerator, new ImageCompressor());
  }

  /** Reads a REST payload and maps it to the domain model, as the controller does. */
  public static ExportAreaPictureAnnotation readPayload(ObjectMapper objectMapper, InputStream json)
      throws IOException {
    var rest =
        objectMapper.readValue(
            json, app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation.class);
    return new ExportAreaPictureAnnotationRestMapper(
            new AreaPictureAnnotationConfRestMapper(),
            new CustomPageMapper(mock(FileService.class)))
        .toDomain(rest);
  }
}
