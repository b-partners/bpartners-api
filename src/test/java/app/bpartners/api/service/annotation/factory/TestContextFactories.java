package app.bpartners.api.service.annotation.factory;

import app.bpartners.api.endpoint.rest.mapper.detection.AreaPictureAnnotationConfRestMapper;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.ImageCompressor;
import app.bpartners.api.service.file.FileService;

public final class TestContextFactories {
  private TestContextFactories() {}

  public static ExportAnnotationContextFactory contextFactory(
      FileService fileService, AreaPictureAnnotationConfRestMapper confMapper) {
    var image3DGenerator = new ExportAreaPictureAnnotationImage3DGenerator();
    return new ExportAnnotationContextFactory(
        new CustomPageMapper(fileService),
        itemImagesResolver(fileService, image3DGenerator),
        image3DGenerator,
        confMapper);
  }

  public static ItemImagesResolver itemImagesResolver(
      FileService fileService, ExportAreaPictureAnnotationImage3DGenerator image3DGenerator) {
    return new ItemImagesResolver(fileService, image3DGenerator, new ImageCompressor());
  }
}
