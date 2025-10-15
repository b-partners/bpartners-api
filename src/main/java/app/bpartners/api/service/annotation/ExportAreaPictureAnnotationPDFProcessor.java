package app.bpartners.api.service.annotation;

import static app.bpartners.api.file.FileWriter.base64Image;
import static app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImageConf.*;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstance;
import app.bpartners.api.model.exception.BadRequestException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportAreaPictureAnnotationPDFProcessor {
  private final ExportAreaPictureAnnotationPDFGenerator exportAreaPictureAnnotationPDFGenerator;
  private final ExportAreaPictureAnnotationImageGenerator exportAreaPictureAnnotationImageGenerator;
  private final ExportAreaPictureAnnotationImageConf mainConf =
      new ExportAreaPictureAnnotationImageConf();
  private static final String IMAGE_FORMAT = "png";
  private static final int SUB_IMAGE_SCALE = 2;
  private final ExportAreaPictureAnnotationImageConf subImageConf =
      new ExportAreaPictureAnnotationImageConf(
          SUB_IMAGE_SCALE,
          DEFAULT_POINT_SIZE,
          DEFAULT_STROKE,
          DEFAULT_POINT_COLOR,
          DEFAULT_MEASUREMENT_BG_COLOR,
          DEFAULT_MEASUREMENT_TEXT_COLOR,
          DEFAULT_MEASUREMENT_OFFSET,
          DEFAULT_MEASUREMENT_FONT);

  public byte[] process(ExportAreaPictureAnnotation exportAnnotation) throws IOException {
    BufferedImage downloadedImage = downloadImage(exportAnnotation.getImageUrl());
    return process(exportAnnotation, downloadedImage);
  }

  public byte[] process(ExportAreaPictureAnnotation exportAnnotation, BufferedImage downloadedImage)
      throws IOException {
    var base64MainImage =
        generateAnnotationImageAsBase64(
            downloadedImage, mainConf, exportAnnotation.getAnnotations());
    List<String> base64SubImages = new ArrayList<>();

    var annotationsByKey =
        ExportAreaPictureAnnotationPDFGenerator.GroupedByKey.from(
            exportAnnotation.getAnnotations());
    for (var annotation : annotationsByKey) {
      base64SubImages.add(
          generateAnnotationImageAsBase64(downloadedImage, subImageConf, annotation.instances()));
    }

    return exportAreaPictureAnnotationPDFGenerator.apply(
        base64MainImage, base64SubImages, exportAnnotation);
  }

  private static BufferedImage downloadImage(String imageUrl) {
    try {
      return ImageIO.read(new URI(imageUrl).toURL());
    } catch (IOException | URISyntaxException e) {
      throw new BadRequestException("Cannot read the image from the url");
    }
  }

  private String generateAnnotationImageAsBase64(
      BufferedImage image,
      ExportAreaPictureAnnotationImageConf conf,
      List<ExportAreaPictureAnnotationInstance> annotations)
      throws IOException {
    BufferedImage generateImage =
        exportAreaPictureAnnotationImageGenerator.apply(image, conf, annotations);
    var outputStream = new ByteArrayOutputStream();
    ImageIO.write(generateImage, IMAGE_FORMAT, outputStream);
    return base64Image(outputStream.toByteArray());
  }
}
