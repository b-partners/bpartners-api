package app.bpartners.api.service.annotation;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.annotation.factory.ExportAnnotationContextFactory;
import app.bpartners.api.service.annotation.model.RenderedImages;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.svgsupport.BatikSVGDrawer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class AnnotationReportPdfRenderer {
  private final TemplateResolverEngine templateResolverEngine;
  private final EmojiReplacer emojiReplacer;
  private final ExportAnnotationContextFactory contextFactory;

  public static final String FONT_NAME = "Kumbh Sans";
  private static final String EMOJI_PATH = "fonts/twemoji/v/14.0.2/svg";
  private static final String FONT_PATH = "fonts/KumbhSans-Regular.ttf";
  private static final String AREA_PICTURE_ANNOTATION_TEMPLATE = "export-area-picture-annotations";

  static {
    SymbolFontFallbackMapper.installOnce(FONT_PATH);
  }

  public AnnotationReportPdfRenderer(
      TemplateResolverEngine templateResolverEngine,
      ExportAnnotationContextFactory contextFactory) {
    this.templateResolverEngine = templateResolverEngine;
    this.contextFactory = contextFactory;
    this.emojiReplacer = createEmojiReplacer();
  }

  @SneakyThrows
  public byte[] apply(
      User user,
      String logoBase64,
      ExportAreaPictureAnnotation annotation,
      RenderedImages annotationImages,
      RenderedImages annotation3DImages) {

    var html =
        parseDataToString(user, logoBase64, annotation, annotationImages, annotation3DImages);
    if (annotation.getLlm() != null) {
      html = emojiReplacer.replaceEmoji(html);
    }

    try (var outputStream = new ByteArrayOutputStream()) {
      var builder = new PdfRendererBuilder();
      builder.useFastMode();
      builder.withHtmlContent(html, null);
      builder.useSVGDrawer(new BatikSVGDrawer());
      builder.useFont(
          () -> {
            try {
              return new ClassPathResource(FONT_PATH).getInputStream();
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          },
          FONT_NAME);

      builder.toStream(outputStream);
      builder.run();

      return outputStream.toByteArray();
    } catch (RuntimeException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private String parseDataToString(
      User user,
      String logoBase64,
      ExportAreaPictureAnnotation annotation,
      RenderedImages annotationImages,
      RenderedImages annotation3DImages) {
    var templateEngine = templateResolverEngine.getTemplateEngine();

    var context =
        contextFactory.createContext(
            user, logoBase64, annotation, annotationImages, annotation3DImages);
    return templateEngine.process(AREA_PICTURE_ANNOTATION_TEMPLATE, context);
  }

  private static EmojiReplacer createEmojiReplacer() {
    try {
      return new EmojiReplacer(EMOJI_PATH, "<span class=\"emoji\">", "</span>");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
