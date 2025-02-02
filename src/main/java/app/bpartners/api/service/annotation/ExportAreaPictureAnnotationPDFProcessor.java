package app.bpartners.api.service.annotation;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExportAreaPictureAnnotationPDFProcessor {
  private final ExportAreaPictureAnnotationPDFGenerator exportAreaPictureAnnotationPDFGenerator;
  private final ExportAreaPictureAnnotationImageGenerator exportAreaPictureAnnotationImageGenerator;
  private static final String IMAGE_FORMAT = "png";

  public byte[] process(ExportAreaPictureAnnotation annotation) throws IOException {
    var areaPictureAnnotationImage = generateAnnotationImage(annotation);
    return exportAreaPictureAnnotationPDFGenerator.apply(areaPictureAnnotationImage, annotation);
  }

  private byte[] generateAnnotationImage(ExportAreaPictureAnnotation annotation)
      throws IOException {
    BufferedImage image = exportAreaPictureAnnotationImageGenerator.apply(annotation);
    var outputStream = new ByteArrayOutputStream();
    ImageIO.write(image, IMAGE_FORMAT, outputStream);
    return outputStream.toByteArray();
  }
}
