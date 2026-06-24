package app.bpartners.api.service.annotation;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.model.User;
import app.bpartners.api.service.annotation.model.Pair;
import com.denisfesenko.converter.HtmlToOpenXMLConverter;
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
