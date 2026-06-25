package app.bpartners.api.service.annotation;

import static app.bpartners.api.service.annotation.ExportAreaPictureAnnotationAdjustment.adjustAnnotation;
import static app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFProcessor.downloadImage;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.model.Pair;
import com.denisfesenko.converter.HtmlToOpenXMLConverter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExportAreaPictureAnnotationDOCXProcessor {
  private final ExportAreaPictureAnnotationPDFGenerator pdfGenerator;
  private final ExportAreaPictureAnnotationPDFProcessor pdfProcessor;
  private final ImageCompressor imageCompressor;

  public byte[] process(
      User user, ExportAreaPictureAnnotation annotation, byte[] global3DImageBytes) {
    BufferedImage mainImage = downloadImage(annotation.getImageUrl());
    BufferedImage compressedMainImage =
        mainImage == null ? null : imageCompressor.compressImage(mainImage);
    var annotationRescale = adjustAnnotation(annotation, mainImage, compressedMainImage);
    String logoBase64 = pdfProcessor.getLogoBase64(user, annotationRescale);
    Pair<String, List<String>> annotationImages =
        pdfProcessor.generateAnnotationImages(
            annotation, compressedMainImage, annotationRescale.x(), annotationRescale.y());
    Pair<String, List<String>> annotation3DImages =
        pdfProcessor.generateAnnotation3DImages(annotation, global3DImageBytes);

    var html =
        pdfGenerator.parseDataToHTML(
            user, logoBase64, annotation, annotationImages, annotation3DImages);
    try {
      HtmlToOpenXMLConverter converter = new HtmlToOpenXMLConverter();
      WordprocessingMLPackage wordDocument = converter.convert(html);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      wordDocument.save(baos);

      return baos.toByteArray();
    } catch (Docx4JException e) {
      throw new RuntimeException(e);
    }
  }

  public byte[] process(
      User user,
      String logoBase64,
      ExportAreaPictureAnnotation annotation,
      Pair<String, List<String>> annotationImages,
      Pair<String, List<String>> annotation3DImages) {
    var html =
        pdfGenerator.parseDataToHTML(
            user, logoBase64, annotation, annotationImages, annotation3DImages);
    try {
      HtmlToOpenXMLConverter converter = new HtmlToOpenXMLConverter();
      WordprocessingMLPackage wordDocument = converter.convert(html);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      wordDocument.save(baos);

      return baos.toByteArray();
    } catch (Docx4JException e) {
      throw new RuntimeException(e);
    }
  }
}
