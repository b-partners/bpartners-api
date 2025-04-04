package app.bpartners.api.service.annotation;

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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExportAreaPictureAnnotationPDFProcessor {
  private final ExportAreaPictureAnnotationPDFGenerator exportAreaPictureAnnotationPDFGenerator;
  private final ExportAreaPictureAnnotationImageGenerator exportAreaPictureAnnotationImageGenerator;
  private final ExportAreaPictureAnnotationImageConf mainConf =
      new ExportAreaPictureAnnotationImageConf();
  private final ExportAreaPictureAnnotationImageConf subImageConf =
      new ExportAreaPictureAnnotationImageConf(true);
  private static final String IMAGE_FORMAT = "png";

  public byte[] process(ExportAreaPictureAnnotation exportAnnotation) throws IOException {
    BufferedImage downloadedImage = downloadImage(exportAnnotation.getImageUrl());
    var mainImage =
        generateAnnotationImage(downloadedImage, mainConf, exportAnnotation.getAnnotations());
    List<byte[]> subImages = new ArrayList<>();

    for (ExportAreaPictureAnnotationInstance annotation : exportAnnotation.getAnnotations()) {
      subImages.add(generateAnnotationImage(downloadedImage, subImageConf, List.of(annotation)));
    }

    return exportAreaPictureAnnotationPDFGenerator.apply(mainImage, subImages, exportAnnotation);
  }

  private static BufferedImage downloadImage(String imageUrl) {
    try {
      return ImageIO.read(new URI(imageUrl).toURL());
    } catch (IOException | URISyntaxException e) {
      throw new BadRequestException("Cannot read the image from the url");
    }
  }

  private byte[] generateAnnotationImage(
      BufferedImage image,
      ExportAreaPictureAnnotationImageConf conf,
      List<ExportAreaPictureAnnotationInstance> annotations)
      throws IOException {
    BufferedImage generateImage =
        exportAreaPictureAnnotationImageGenerator.apply(image, conf, annotations);
    var outputStream = new ByteArrayOutputStream();
    ImageIO.write(generateImage, IMAGE_FORMAT, outputStream);
    return outputStream.toByteArray();
  }
}
