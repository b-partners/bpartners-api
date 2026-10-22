package app.bpartners.api.service.annotation;

import static app.bpartners.api.file.FileWriter.base64Image;
import static app.bpartners.api.service.annotation.ExportAreaPictureAnnotationAdjustment.adjustAnnotation;
import static app.bpartners.api.service.annotation.utils.ImageUriUtils.base64;
import static app.bpartners.api.service.utils.UserUtils.getUserLogo;

import app.bpartners.api.endpoint.rest.mapper.detection.AreaPictureAnnotationConfRestMapper;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3D;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstance;
import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationAdjustment.RescaleValue;
import app.bpartners.api.service.annotation.model.GroupedByKey;
import app.bpartners.api.service.annotation.model.RenderedImages;
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
public class AnnotationReportService {
  private final AnnotationReportPdfRenderer annotationReportPdfRenderer;
  private final ExportAreaPictureAnnotationImageGenerator exportAreaPictureAnnotationImageGenerator;
  private final ExportAreaPictureAnnotationImage3DGenerator
      exportAreaPictureAnnotationImage3DGenerator;
  private final FileService fileService;
  private final ImageCompressor imageCompressor;
  private final AreaPictureAnnotationConfRestMapper areaPictureAnnotationConfRestMapper;
  private final ImageDownloader imageDownloader;

  private static final int SUB_IMAGE_SCALE = 2;

  private static ExportAreaPictureAnnotationImageConf mainConf() {
    return new ExportAreaPictureAnnotationImageConf();
  }

  private static ExportAreaPictureAnnotationImageConf subImageConf() {
    var subImageConf = new ExportAreaPictureAnnotationImageConf();
    subImageConf.setScale(SUB_IMAGE_SCALE);
    return subImageConf;
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
            ? resolveGlobalImage3D(globalImage3D, exportAnnotation.getGlobalImage3DUrl())
            : null;
    return process(user, exportAnnotation, conf, downloadedImage, resolvedGlobalImage3D);
  }

  public byte[] process(
      User user,
      ExportAreaPictureAnnotation exportAnnotation,
      BufferedImage downloadedImage,
      byte[] globalImage3D)
      throws IOException {
    var conf = areaPictureAnnotationConfRestMapper.toDomain(exportAnnotation.getConf());
    var image =
        downloadedImage == null
            ? null
            : new ImageDownloader.DownloadedImage(
                downloadedImage, downloadedImage.getWidth(), downloadedImage.getHeight());
    return process(user, exportAnnotation, conf, image, globalImage3D);
  }

  private byte[] resolveGlobalImage3D(byte[] provided, String url) {
    if (provided != null) {
      return provided;
    }
    return url == null ? null : imageDownloader.downloadImageBytes(url);
  }

  private byte[] process(
      User user,
      ExportAreaPictureAnnotation exportAnnotation,
      ExportAreaPictureAnnotationConf conf,
      ImageDownloader.DownloadedImage downloadedImage,
      byte[] globalImage3D) {
    BufferedImage compressedImage =
        downloadedImage == null ? null : imageCompressor.compressImage(downloadedImage.image());
    var annotationRescale =
        adjustAnnotation(
            exportAnnotation,
            downloadedImage == null ? 0 : downloadedImage.trueWidth(),
            downloadedImage == null ? 0 : downloadedImage.trueHeight(),
            compressedImage);
    RenderedImages annotationImages =
        generateAnnotationImages(exportAnnotation, compressedImage, annotationRescale, conf);

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

    RenderedImages annotation3DImages = null;

    if (conf.isShowAnnotation3dPages()
        && exportAnnotation.get3d() != null
        && globalImage3D != null) {
      byte[] compressedGlobalImage3D = imageCompressor.compressImage(globalImage3D);
      annotation3DImages =
          generateAnnotation3DImages(exportAnnotation.get3d(), compressedGlobalImage3D);
    }

    return annotationReportPdfRenderer.apply(
        user, logoBase64, exportAnnotation, annotationImages, annotation3DImages);
  }

  private RenderedImages generateAnnotation3DImages(
      ExportAreaPictureAnnotation3D annotation3D, byte[] globalImage3D) {
    var mainImage3D = base64Image(globalImage3D);
    var subImages3D = new ArrayList<String>();

    for (var pan : annotation3D.getPans()) {
      var panImage =
          exportAreaPictureAnnotationImage3DGenerator.generatePanImageWithMeasurements(pan, true);
      var compressedPanImage = imageCompressor.compressImage(panImage);
      subImages3D.add(base64(compressedPanImage));
    }

    return new RenderedImages(mainImage3D, subImages3D);
  }

  private RenderedImages generateAnnotationImages(
      ExportAreaPictureAnnotation annotation,
      BufferedImage baseImage,
      RescaleValue rescale,
      ExportAreaPictureAnnotationConf conf) {
    boolean needsMainImage = conf.isShowTitlePage();
    boolean needsSubImages = conf.isShowAnnotationPages();
    if (baseImage == null || (!needsMainImage && !needsSubImages)) {
      return new RenderedImages(null, List.of());
    }

    String mainImage =
        needsMainImage
            ? generateAnnotationImageAsBase64(
                baseImage,
                mainConf().rescale(rescale.x(), rescale.y()),
                annotation.getAnnotations())
            : null;
    var subImages = new ArrayList<String>();

    if (needsSubImages) {
      var annotationsByKey = GroupedByKey.from(annotation.getAnnotations());
      for (var item : annotationsByKey) {
        subImages.add(
            generateAnnotationImageAsBase64(
                baseImage, subImageConf().rescale(rescale.x(), rescale.y()), item.instances()));
      }
    }

    return new RenderedImages(mainImage, subImages);
  }

  private String generateAnnotationImageAsBase64(
      BufferedImage image,
      ExportAreaPictureAnnotationImageConf conf,
      List<ExportAreaPictureAnnotationInstance> annotations) {
    var generatedImage = exportAreaPictureAnnotationImageGenerator.apply(image, conf, annotations);
    return base64(generatedImage);
  }
}
