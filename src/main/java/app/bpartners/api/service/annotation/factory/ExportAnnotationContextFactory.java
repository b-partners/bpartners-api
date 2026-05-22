package app.bpartners.api.service.annotation.factory;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3D;
import app.bpartners.api.file.bucket.BucketComponent;
import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFGenerator;
import app.bpartners.api.service.annotation.model.Pair;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.thymeleaf.context.Context;

@Slf4j
public class ExportAnnotationContextFactory {
  private static final String BASE_64_URI_PREFIX = "data:image/png;base64,";
  public static final String IMAGE_FORMAT = "png";

  public static Context createContext(
      User user,
      String logoBase64,
      ExportAreaPictureAnnotation annotation,
      Pair<String, List<String>> annotationImages,
      Pair<String, List<String>> annotation3DImages,
      BucketComponent bucketComponent) {
    var context = new Context();

    var logoUri = logoBase64 == null ? null : base64ToUri(logoBase64);
    var mainImageUri = base64ToUri(annotationImages.first());
    var subImagesUris =
        annotationImages.second().stream()
            .map(ExportAnnotationContextFactory::base64ToUri)
            .toList();

    context.setVariable("user", user);
    context.setVariable("userWebsite", user.getDefaultWebsite());
    context.setVariable("logo", logoUri);
    context.setVariable("address", annotation.getAddress());
    context.setVariable("mainImage", mainImageUri);
    context.setVariable(
        "pages",
        groupByFirstPage(
            ExportAreaPictureAnnotationPDFGenerator.GroupedByKey.from(annotation.getAnnotations()),
            3,
            3));
    context.setVariable("subImagesPages", groupByFirstPage(subImagesUris, 3, 3));

    if (annotation.getLlm() != null) {
      configureLLMContext(context, annotation);
    }

    if (annotation.getGlobalRateValue() != null || annotation.getGlobalRateType() != null) {
      configureGlobalRateContext(context, annotation);
    }
    if (annotation.get3d() != null) {
      configureAnnotation3DContext(
          context, annotation.get3d(), annotation3DImages, bucketComponent);
    }

    return context;
  }

  static void configureLLMContext(Context context, ExportAreaPictureAnnotation annotation) {
    context.setVariable("llm", annotation.getLlm());
  }

  static void configureGlobalRateContext(Context context, ExportAreaPictureAnnotation annotation) {
    context.setVariable("globalRateType", annotation.getGlobalRateType());
    context.setVariable("globalRateValue", annotation.getGlobalRateValue());
    context.setVariable(
        "degradationLevels",
        List.of(
            Map.of("label", "A", "color", "#47BE62"),
            Map.of("label", "B", "color", "#F4FBAB"),
            Map.of("label", "C", "color", "#F9DD56"),
            Map.of("label", "D", "color", "#F38F4B"),
            Map.of("label", "E", "color", "#EF2C2D")));
  }

  static void configureAnnotation3DContext(
      Context context,
      ExportAreaPictureAnnotation3D annotation3D,
      Pair<String, List<String>> annotation3DImages,
      BucketComponent bucketComponent) {
    var pages3D = groupByFirstPage(annotation3D.getPans(), 3, 4);
    var mainImage3DUri = base64ToUri(annotation3DImages.first());
    var subImages3DUris =
        annotation3DImages.second().stream()
            .map(ExportAnnotationContextFactory::base64ToUri)
            .toList();
    var pansImages3D = getPansImages3DContext(annotation3D, bucketComponent);

    context.setVariable("pages3D", pages3D);
    context.setVariable("mainImage3D", mainImage3DUri);
    context.setVariable("subImagesPages3D", groupByFirstPage(subImages3DUris, 3, 4));
    context.setVariable("pansImages3DUris", groupByFirstPage(pansImages3D, 3, 4));
  }

  static List<String> getPansImages3DContext(
      ExportAreaPictureAnnotation3D annotation3D, BucketComponent bucketComponent) {
    var exportAreaPictureAnnotationImage3DGenerator =
        new ExportAreaPictureAnnotationImage3DGenerator();

    var overallPansTopView =
        exportAreaPictureAnnotationImage3DGenerator.generateBaseImage(annotation3D.getPans());
    return annotation3D.getPans().stream()
        .map(
            pan -> {
              var image =
                  exportAreaPictureAnnotationImage3DGenerator.generateBaseImageWithHighlightedPan(
                      overallPansTopView.second(), overallPansTopView.first(), pan);
              try {
                if (pan.getImageUri() == null || pan.getImageUri().isBlank()) {
                  log.warn(
                      "No image provided for pan: {}. Falling back to top view image.",
                      pan.getName());
                  return bufferedImageToUri(image);
                }
                var imageFileFromS3 = bucketComponent.download(pan.getImageUri(), true);

                if (imageFileFromS3 != null) {
                  image = ImageIO.read(imageFileFromS3);
                }
              } catch (IOException e) {
                log.error(
                    "Error while downloading pan image: {}. Falling back to top view image.",
                    pan.getName(),
                    e);
              }

              return bufferedImageToUri(image);
            })
        .toList();
  }

  private static String bufferedImageToUri(BufferedImage image) {
    try {
      return base64ToUri(base64(image));
    } catch (IOException e) {
      throw new IllegalStateException("Could not convert image to base64 uri", e);
    }
  }

  static <T> List<List<T>> groupByFirstPage(List<T> list, int firstPageMax, int limit) {
    List<List<T>> pages = new ArrayList<>();
    var iterator = list.iterator();

    List<T> firstPage = new ArrayList<>();
    for (int i = 0; i < firstPageMax && iterator.hasNext(); i++) {
      firstPage.add(iterator.next());
    }
    if (!firstPage.isEmpty()) {
      pages.add(firstPage);
    }

    while (iterator.hasNext()) {
      List<T> page = new ArrayList<>();
      for (int i = 0; i < limit && iterator.hasNext(); i++) {
        page.add(iterator.next());
      }
      pages.add(page);
    }

    return pages;
  }

  public static String base64(BufferedImage image) throws IOException {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStream b64 = Base64.getEncoder().wrap(out)) {

      ImageIO.write(image, IMAGE_FORMAT, b64);
      b64.flush();
      return out.toString(StandardCharsets.ISO_8859_1);
    }
  }

  static String base64ToUri(String base64Image) {
    return !base64Image.startsWith("data:") ? BASE_64_URI_PREFIX + base64Image : base64Image;
  }
}
