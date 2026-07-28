package app.bpartners.api.service.annotation.export;

import static app.bpartners.api.file.FileWriter.base64Image;
import static app.bpartners.api.service.annotation.export.AreaAnnotationAdjustment.adjustAnnotation;
import static app.bpartners.api.service.annotation.export.AreaAnnotationImageConf.*;
import static app.bpartners.api.service.annotation.utils.ImageUriUtils.base64;
import static app.bpartners.api.service.utils.UserUtils.getUserLogo;

import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.annotation.AreaAnnotation3D;
import app.bpartners.api.service.annotation.AreaAnnotationExportPayload;
import app.bpartners.api.service.annotation.AreaAnnotationInstance;
import app.bpartners.api.service.annotation.ImageCompressor;
import app.bpartners.api.service.annotation.export.AreaAnnotationAdjustment.AdjustedAnnotationResult;
import app.bpartners.api.service.annotation.export.AreaAnnotationPDFGenerator.GroupedByKey;
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
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AreaAnnotationPDFProcessor {
  private final AreaAnnotationPDFGenerator areaAnnotationPDFGenerator;
  private final AreaAnnotationImageGenerator areaAnnotationImageGenerator;
  private final AreaAnnotationImage3DGenerator areaAnnotationImage3DGenerator;
  private final FileService fileService;
  private final ImageCompressor imageCompressor;

  private static AreaAnnotationImageConf mainConf() {
    return new AreaAnnotationImageConf();
  }

  private static AreaAnnotationImageConf subImageConf() {
    return new AreaAnnotationImageConf(
        2,
        DEFAULT_POINT_SIZE,
        DEFAULT_STROKE,
        DEFAULT_POINT_COLOR,
        DEFAULT_MEASUREMENT_BG_COLOR,
        DEFAULT_MEASUREMENT_TEXT_COLOR,
        DEFAULT_MEASUREMENT_OFFSET,
        DEFAULT_MEASUREMENT_FONT);
  }

  public byte[] process(User user, AreaAnnotationExportPayload exportAnnotation)
      throws IOException {
    return process(user, exportAnnotation, null);
  }

  public byte[] process(
      User user, AreaAnnotationExportPayload exportAnnotation, byte[] globalImage3D)
      throws IOException {
    BufferedImage downloadedImage = downloadImage(exportAnnotation.getImageUrl());
    return process(user, exportAnnotation, downloadedImage, globalImage3D);
  }

  public byte[] process(
      User user,
      AreaAnnotationExportPayload exportAnnotation,
      BufferedImage downloadedImage,
      byte[] globalImage3D)
      throws IOException {
    BufferedImage compressedImage =
        downloadedImage == null ? null : imageCompressor.compressImage(downloadedImage);
    AdjustedAnnotationResult adjustmentResult =
        adjustAnnotation(exportAnnotation, downloadedImage, compressedImage);
    var adjustedAnnotation = adjustmentResult.adjustedAnnotation();
    var rescaleValue = adjustmentResult.rescaleValue();

    Pair<String, List<String>> annotationImages =
        generateAnnotationImages(
            adjustedAnnotation, compressedImage, rescaleValue.x(), rescaleValue.y());

    Pair<String, List<String>> annotation3DImages = null;
    BufferedImage logo = getUserLogo(user.getId(), user.getLogoFileId(), fileService);
    String logoBase64 =
        logo == null
            ? null
            : generateAnnotationImageAsBase64(
                logo, subImageConf().rescale(rescaleValue.x(), rescaleValue.y()), List.of());

    Pair<String, List<String>> annotation3DFacadeImages = null;

    if (adjustedAnnotation.getAnnotation3d() != null && globalImage3D != null) {
      annotation3DImages =
          generateAnnotation3DImages(adjustedAnnotation.getAnnotation3d(), globalImage3D);
      annotation3DFacadeImages =
          generateAnnotation3DFacadeImages(adjustedAnnotation.getAnnotation3d(), globalImage3D);
    }

    return areaAnnotationPDFGenerator.apply(
        user,
        logoBase64,
        adjustedAnnotation,
        annotationImages,
        annotation3DImages,
        annotation3DFacadeImages);
  }

  private Pair<String, List<String>> generateAnnotation3DImages(
      AreaAnnotation3D annotation3D, byte[] globalImage3D) {
    var mainImage3D = base64Image(globalImage3D);
    var subImages3D = new ArrayList<String>();

    for (var pan : annotation3D.getPans()) {
      var panImage = areaAnnotationImage3DGenerator.generatePanImageWithMeasurements(pan);
      subImages3D.add(base64(panImage));
    }

    return new Pair<>(mainImage3D, subImages3D);
  }

  private Pair<String, List<String>> generateAnnotation3DFacadeImages(
      AreaAnnotation3D annotation3D, byte[] globalImage3D) {
    var mainImage3D = base64Image(globalImage3D);
    var subImages3D = new ArrayList<String>();

    if (annotation3D.getFacades() != null) {
      for (var facade : annotation3D.getFacades()) {
        var facadeImage = areaAnnotationImage3DGenerator.generatePanImageWithMeasurements(facade);
        subImages3D.add(base64(facadeImage));
      }
    }

    return new Pair<>(mainImage3D, subImages3D);
  }

  private Pair<String, List<String>> generateAnnotationImages(
      AreaAnnotationExportPayload annotation,
      BufferedImage baseImage,
      double rescaleXValue,
      double rescaleYValue)
      throws IOException {
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
      BufferedImage image, AreaAnnotationImageConf conf, List<AreaAnnotationInstance> annotations) {
    var generatedImage = areaAnnotationImageGenerator.apply(image, conf, annotations);
    return base64(generatedImage);
  }

  private static BufferedImage downloadImage(String imageUrl) {
    try {
      return ImageIO.read(new URI(imageUrl).toURL());
    } catch (IOException | URISyntaxException e) {
      throw new BadRequestException("Cannot read the image from the url");
    }
  }
}
