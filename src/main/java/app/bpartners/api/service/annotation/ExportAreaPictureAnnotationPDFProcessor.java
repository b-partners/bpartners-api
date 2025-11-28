package app.bpartners.api.service.annotation;

import static app.bpartners.api.file.FileWriter.base64Image;
import static app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImageConf.*;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3D;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstance;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFGenerator.GroupedByKey;
import app.bpartners.api.service.annotation.model.Pair;
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
  private final ExportAreaPictureAnnotationImage3DGenerator
      exportAreaPictureAnnotationImage3DGenerator;

  private static final String IMAGE_FORMAT = "png";

  private static final ExportAreaPictureAnnotationImageConf ANNOTATION_MAIN_CONF =
      new ExportAreaPictureAnnotationImageConf();
  private static final ExportAreaPictureAnnotationImageConf ANNOTATION_SUB_IMAGE_CONF =
      new ExportAreaPictureAnnotationImageConf(
          2,
          DEFAULT_POINT_SIZE,
          DEFAULT_STROKE,
          DEFAULT_POINT_COLOR,
          DEFAULT_MEASUREMENT_BG_COLOR,
          DEFAULT_MEASUREMENT_TEXT_COLOR,
          DEFAULT_MEASUREMENT_OFFSET,
          DEFAULT_MEASUREMENT_FONT);

  public byte[] process(ExportAreaPictureAnnotation exportAnnotation) throws IOException {
    return process(exportAnnotation, null);
  }

  public byte[] process(ExportAreaPictureAnnotation exportAnnotation, byte[] globalImage3D)
      throws IOException {
    BufferedImage downloadedImage = downloadImage(exportAnnotation.getImageUrl());
    return process(exportAnnotation, downloadedImage, globalImage3D);
  }

  public byte[] process(
      ExportAreaPictureAnnotation exportAnnotation,
      BufferedImage downloadedImage,
      byte[] globalImage3D)
      throws IOException {
    Pair<String, List<String>> annotationImages =
        generateAnnotationImages(exportAnnotation, downloadedImage);
    Pair<String, List<String>> annotation3DImages = null;

    if (exportAnnotation.get3d() != null && globalImage3D != null) {
      annotation3DImages = generateAnnotation3DImages(exportAnnotation.get3d(), globalImage3D);
    }

    return exportAreaPictureAnnotationPDFGenerator.apply(
        exportAnnotation, annotationImages, annotation3DImages);
  }

  private Pair<String, List<String>> generateAnnotation3DImages(
      ExportAreaPictureAnnotation3D annotation3D, byte[] globalImage3D) throws IOException {
    var mainImage3D = base64Image(globalImage3D);
    var baseImageData =
        exportAreaPictureAnnotationImage3DGenerator.generateBaseImage(annotation3D.getPans());

    var subImages3D = new ArrayList<String>();
    for (var pan : annotation3D.getPans()) {
      var panImage =
          exportAreaPictureAnnotationImage3DGenerator.generatePanImage(
              baseImageData.second(), baseImageData.first(), pan);
      subImages3D.add(base64(panImage));
    }

    return new Pair<>(mainImage3D, subImages3D);
  }

  private Pair<String, List<String>> generateAnnotationImages(
      ExportAreaPictureAnnotation annotation, BufferedImage baseImage) throws IOException {
    var mainImage =
        generateAnnotationImageAsBase64(
            baseImage, ANNOTATION_MAIN_CONF, annotation.getAnnotations());
    var subImages = new ArrayList<String>();
    var annotationsByKey = GroupedByKey.from(annotation.getAnnotations());

    for (var item : annotationsByKey) {
      subImages.add(
          generateAnnotationImageAsBase64(baseImage, ANNOTATION_SUB_IMAGE_CONF, item.instances()));
    }

    return new Pair<>(mainImage, subImages);
  }

  private String generateAnnotationImageAsBase64(
      BufferedImage image,
      ExportAreaPictureAnnotationImageConf conf,
      List<ExportAreaPictureAnnotationInstance> annotations)
      throws IOException {
    var generatedImage = exportAreaPictureAnnotationImageGenerator.apply(image, conf, annotations);
    return base64(generatedImage);
  }

  private static String base64(BufferedImage bufferedImage) throws IOException {
    var outputStream = new ByteArrayOutputStream();
    ImageIO.write(bufferedImage, IMAGE_FORMAT, outputStream);
    return base64Image(outputStream.toByteArray());
  }

  private static BufferedImage downloadImage(String imageUrl) {
    try {
      return ImageIO.read(new URI(imageUrl).toURL());
    } catch (IOException | URISyntaxException e) {
      throw new BadRequestException("Cannot read the image from the url");
    }
  }
}
