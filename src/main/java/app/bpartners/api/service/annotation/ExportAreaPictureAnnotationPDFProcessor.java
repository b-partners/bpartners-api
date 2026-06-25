package app.bpartners.api.service.annotation;

import static app.bpartners.api.file.FileWriter.base64Image;
import static app.bpartners.api.service.annotation.ExportAreaPictureAnnotationAdjustment.adjustAnnotation;
import static app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImageConf.*;
import static app.bpartners.api.service.annotation.utils.ImageUriUtils.base64;
import static app.bpartners.api.service.utils.UserUtils.getUserLogo;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3D;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstance;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFGenerator.GroupedByKey;
import app.bpartners.api.service.annotation.model.Pair;
import app.bpartners.api.service.file.FileService;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportAreaPictureAnnotationPDFProcessor {
  private final ExportAreaPictureAnnotationPDFGenerator exportAreaPictureAnnotationPDFGenerator;
  private final ExportAreaPictureAnnotationImageGenerator exportAreaPictureAnnotationImageGenerator;
  private final ExportAreaPictureAnnotationImage3DGenerator
      exportAreaPictureAnnotationImage3DGenerator;
  private final FileService fileService;
  private final ImageCompressor imageCompressor;

  private static ExportAreaPictureAnnotationImageConf mainConf() {
    return new ExportAreaPictureAnnotationImageConf();
  }

  static ExportAreaPictureAnnotationImageConf subImageConf() {
    return new ExportAreaPictureAnnotationImageConf(
        2,
        DEFAULT_POINT_SIZE,
        DEFAULT_STROKE,
        DEFAULT_POINT_COLOR,
        DEFAULT_MEASUREMENT_BG_COLOR,
        DEFAULT_MEASUREMENT_TEXT_COLOR,
        DEFAULT_MEASUREMENT_OFFSET,
        DEFAULT_MEASUREMENT_FONT);
  }

  public byte[] process(User user, ExportAreaPictureAnnotation exportAnnotation)
      throws IOException {
    return process(user, exportAnnotation, null);
  }

  public byte[] process(
      User user, ExportAreaPictureAnnotation exportAnnotation, byte[] globalImage3D)
      throws IOException {
    BufferedImage downloadedImage = downloadImage(exportAnnotation.getImageUrl());
    return process(user, exportAnnotation, downloadedImage, globalImage3D);
  }

  public byte[] process(
      User user,
      ExportAreaPictureAnnotation exportAnnotation,
      BufferedImage downloadedImage,
      byte[] globalImage3D)
      throws IOException {
    BufferedImage compressedImage =
        downloadedImage == null ? null : imageCompressor.compressImage(downloadedImage);
    var annotationRescale = adjustAnnotation(exportAnnotation, downloadedImage, compressedImage);
    Pair<String, List<String>> annotationImages =
        generateAnnotationImages(
            exportAnnotation, compressedImage, annotationRescale.x(), annotationRescale.y());
    String logoBase64 = getLogoBase64(user, annotationRescale);

    Pair<String, List<String>> annotation3DImages =
        generateAnnotation3DImages(exportAnnotation, globalImage3D);

    return exportAreaPictureAnnotationPDFGenerator.apply(
        user, logoBase64, exportAnnotation, annotationImages, annotation3DImages);
  }

  @Nullable
  Pair<String, List<String>> generateAnnotation3DImages(
      ExportAreaPictureAnnotation exportAnnotation, byte[] globalImage3D) {
    Pair<String, List<String>> annotation3DImages = null;
    if (exportAnnotation.get3d() != null && globalImage3D != null) {
      annotation3DImages = generateAnnotation3DImages(exportAnnotation.get3d(), globalImage3D);
    }
    return annotation3DImages;
  }

  public @Nullable String getLogoBase64(
      User user, ExportAreaPictureAnnotationAdjustment.RescaleValue annotationRescale) {
    BufferedImage logo = getUserLogo(user.getId(), user.getLogoFileId(), fileService);
    String logoBase64 =
        logo == null
            ? null
            : generateAnnotationImageAsBase64(
                logo,
                subImageConf().rescale(annotationRescale.x(), annotationRescale.y()),
                List.of());
    return logoBase64;
  }

  private Pair<String, List<String>> generateAnnotation3DImages(
      ExportAreaPictureAnnotation3D annotation3D, byte[] globalImage3D) {
    var mainImage3D = base64Image(globalImage3D);
    var subImages3D = new ArrayList<String>();

    for (var pan : annotation3D.getPans()) {
      var panImage =
          exportAreaPictureAnnotationImage3DGenerator.generatePanImageWithMeasurements(pan);
      subImages3D.add(base64(panImage));
    }

    return new Pair<>(mainImage3D, subImages3D);
  }

  Pair<String, List<String>> generateAnnotationImages(
      ExportAreaPictureAnnotation annotation,
      BufferedImage baseImage,
      double rescaleXValue,
      double rescaleYValue) {
    var mainImage =
        generateAnnotationImageAsBase64(
            baseImage,
            mainConf().rescale(rescaleXValue, rescaleYValue),
            annotation.getAnnotations());
    var subImages = new ArrayList<String>();
    var annotationsByKey = GroupedByKey.from(annotation.getAnnotations());

    for (var item : annotationsByKey) {
      subImages.add(
          generateAnnotationImageAsBase64(
              baseImage, subImageConf().rescale(rescaleXValue, rescaleYValue), item.instances()));
    }

    return new Pair<>(mainImage, subImages);
  }

  private String generateAnnotationImageAsBase64(
      BufferedImage image,
      ExportAreaPictureAnnotationImageConf conf,
      List<ExportAreaPictureAnnotationInstance> annotations) {
    var generatedImage = exportAreaPictureAnnotationImageGenerator.apply(image, conf, annotations);
    return base64(generatedImage);
  }

  static BufferedImage downloadImage(String imageUrl) {
    try {
      return ImageIO.read(new URI(imageUrl).toURL());
    } catch (IOException | URISyntaxException e) {
      throw new BadRequestException("Cannot read the image from the url");
    }
  }
}
