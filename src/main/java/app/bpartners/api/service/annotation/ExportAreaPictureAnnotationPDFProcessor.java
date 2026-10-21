package app.bpartners.api.service.annotation;

import static app.bpartners.api.file.FileWriter.base64Image;
import static app.bpartners.api.service.annotation.ExportAreaPictureAnnotationAdjustment.adjustAnnotation;
import static app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImageConf.*;
import static app.bpartners.api.service.annotation.utils.ImageUriUtils.base64;
import static app.bpartners.api.service.utils.UserUtils.getUserLogo;

import app.bpartners.api.endpoint.rest.mapper.detection.AreaPictureAnnotationConfRestMapper;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3D;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstance;
import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.model.GroupedByKey;
import app.bpartners.api.service.annotation.model.Pair;
import app.bpartners.api.service.file.FileService;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
  private final FileService fileService;
  private final ImageCompressor imageCompressor;
  private final AreaPictureAnnotationConfRestMapper areaPictureAnnotationConfRestMapper;
  private final ImageDownloader imageDownloader;

  private static ExportAreaPictureAnnotationImageConf mainConf() {
    return new ExportAreaPictureAnnotationImageConf();
  }

  private static ExportAreaPictureAnnotationImageConf subImageConf() {
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
    var conf = areaPictureAnnotationConfRestMapper.toDomain(exportAnnotation.getConf());
    ImageDownloader.DownloadedImage downloadedImage =
        conf.isShowTitlePage() || conf.isShowAnnotationPages()
            ? imageDownloader.downloadImage(exportAnnotation.getImageUrl())
            : null;
    byte[] resolvedGlobalImage3D =
        conf.isShowAnnotation3dPages() && exportAnnotation.get3d() != null
            ? globalImage3D != null
                ? globalImage3D
                : imageDownloader.downloadImageBytes(exportAnnotation.getGlobalImage3DUrl())
            : null;
    return process(
        user,
        exportAnnotation,
        downloadedImage == null ? null : downloadedImage.image(),
        downloadedImage == null ? 0 : downloadedImage.trueWidth(),
        downloadedImage == null ? 0 : downloadedImage.trueHeight(),
        resolvedGlobalImage3D);
  }

  public byte[] process(
      User user,
      ExportAreaPictureAnnotation exportAnnotation,
      BufferedImage downloadedImage,
      byte[] globalImage3D)
      throws IOException {
    return process(
        user,
        exportAnnotation,
        downloadedImage,
        downloadedImage == null ? 0 : downloadedImage.getWidth(),
        downloadedImage == null ? 0 : downloadedImage.getHeight(),
        globalImage3D);
  }

  private byte[] process(
      User user,
      ExportAreaPictureAnnotation exportAnnotation,
      BufferedImage downloadedImage,
      int trueWidth,
      int trueHeight,
      byte[] globalImage3D)
      throws IOException {
    var conf = areaPictureAnnotationConfRestMapper.toDomain(exportAnnotation.getConf());
    BufferedImage compressedImage =
        downloadedImage == null ? null : imageCompressor.compressImage(downloadedImage);
    var annotationRescale =
        adjustAnnotation(exportAnnotation, trueWidth, trueHeight, compressedImage);
    Pair<String, List<String>> annotationImages =
        generateAnnotationImages(
            exportAnnotation,
            compressedImage,
            annotationRescale.x(),
            annotationRescale.y(),
            conf.isShowTitlePage(),
            conf.isShowAnnotationPages());

    String logoBase64 = null;
    if (conf.isShowTitlePage()) {
      BufferedImage logo = getUserLogo(user.getId(), user.getLogoFileId(), fileService);
      logoBase64 =
          logo == null
              ? null
              : generateAnnotationImageAsBase64(
                  logo,
                  subImageConf().rescale(annotationRescale.x(), annotationRescale.y()),
                  List.of());
    }

    Pair<String, List<String>> annotation3DImages = null;

    if (conf.isShowAnnotation3dPages()
        && exportAnnotation.get3d() != null
        && globalImage3D != null) {
      byte[] compressedGlobalImage3D = imageCompressor.compressImage(globalImage3D);
      annotation3DImages =
          generateAnnotation3DImages(exportAnnotation.get3d(), compressedGlobalImage3D);
    }

    return exportAreaPictureAnnotationPDFGenerator.apply(
        user, logoBase64, exportAnnotation, annotationImages, annotation3DImages);
  }

  private Pair<String, List<String>> generateAnnotation3DImages(
      ExportAreaPictureAnnotation3D annotation3D, byte[] globalImage3D) {
    var mainImage3D = base64Image(globalImage3D);
    var subImages3D = new ArrayList<String>();

    for (var pan : annotation3D.getPans()) {
      var panImage =
          exportAreaPictureAnnotationImage3DGenerator.generatePanImageWithMeasurements(pan, true);
      var compressedPanImage = imageCompressor.compressImage(panImage);
      subImages3D.add(base64(compressedPanImage));
    }

    return new Pair<>(mainImage3D, subImages3D);
  }

  private Pair<String, List<String>> generateAnnotationImages(
      ExportAreaPictureAnnotation annotation,
      BufferedImage baseImage,
      double rescaleXValue,
      double rescaleYValue,
      boolean needsMainImage,
      boolean needsSubImages) {
    if (baseImage == null || (!needsMainImage && !needsSubImages)) {
      return new Pair<>(null, List.of());
    }

    String mainImage =
        needsMainImage
            ? generateAnnotationImageAsBase64(
                baseImage,
                mainConf().rescale(rescaleXValue, rescaleYValue),
                annotation.getAnnotations())
            : null;
    var subImages = new ArrayList<String>();

    if (needsSubImages) {
      var annotationsByKey = GroupedByKey.from(annotation.getAnnotations());
      for (var item : annotationsByKey) {
        subImages.add(
            generateAnnotationImageAsBase64(
                baseImage, subImageConf().rescale(rescaleXValue, rescaleYValue), item.instances()));
      }
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
}
