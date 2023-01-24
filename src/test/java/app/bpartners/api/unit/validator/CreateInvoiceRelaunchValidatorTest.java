package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.CreateInvoiceRelaunch;
import app.bpartners.api.endpoint.rest.validator.CreateInvoiceRelaunchValidator;
import java.util.List;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CreateInvoiceRelaunchValidatorTest {
  private static final String MAL_FORMED_HTML_EXCEPTION =
      "Your HTML syntax is malformed or you use other tags than these allowed : "
          + allowedTags();
  private final CreateInvoiceRelaunchValidator subject = new CreateInvoiceRelaunchValidator();

  private static String allowedTags() {
    return "a, b, blockquote, br, caption, cite, code, col, colgroup, dd, "
        + "div, dl, dt, em, h1, h2, h3, h4, h5, h6, i, img, li, ol, p, pre, q, "
        + "small, span, strike, strong, sub, sup, table, tbody, td, tfoot, th, "
        + "thead, tr, u, ul, del";
  }

  CreateInvoiceRelaunch baseCreateInvoiceRelaunch() {
    return new CreateInvoiceRelaunch()
        .attachments(List.of());
  }

  CreateInvoiceRelaunch messageWithTextPlain() {
    return baseCreateInvoiceRelaunch().emailBody("Text plain")._object("Custom mail object");
  }

  CreateInvoiceRelaunch messageCorrectlyFormed() {
    return baseCreateInvoiceRelaunch().emailBody(
        "<p><strong><i><em><del>Hello</del></em></i></strong></p>");
  }

  CreateInvoiceRelaunch messageWithNotClosedTags() {
    return baseCreateInvoiceRelaunch().emailBody("<p><p><i><em>Hello</em></i></p>");
  }

  CreateInvoiceRelaunch messageMalFormed() {
    return baseCreateInvoiceRelaunch().emailBody("<p Hello </p>");
  }

  CreateInvoiceRelaunch messageWithBadTags() {
    return baseCreateInvoiceRelaunch().emailBody("<img> Hello");
  }

  @Test
  void invoice_relaunch_has_allowed_tags_correctly_formed_ok() {
    assertDoesNotThrow(
        () -> subject.accept(messageCorrectlyFormed())
    );
  }

  @Test
  void invoice_relaunch_has_text_plain_ok() {
    assertDoesNotThrow(
        () -> subject.accept(messageWithTextPlain())
    );
  }

  @Test
  void invoice_relaunch_has_allowed_tags_mal_formed_ok() {
    assertDoesNotThrow(
        () -> subject.accept(messageWithNotClosedTags())
    );
  }

  @Test
  void invoice_relaunch_has_mal_formed_tags_ko() {
    assertThrowsBadRequestException(
        MAL_FORMED_HTML_EXCEPTION,
        () -> subject.accept(messageMalFormed())
    );
  }

  @Test
  void invoice_relaunch_has_bad_tags_ko() {
    assertThrowsBadRequestException(
        MAL_FORMED_HTML_EXCEPTION,
        () -> subject.accept(messageWithBadTags())
    );
  }

  @Test
  void invoice_relaunch_has_null_attachments_ok() {
    CreateInvoiceRelaunch invoice = messageCorrectlyFormed().attachments(null);

    subject.accept(invoice);

    assertEquals(List.of(), invoice.getAttachments());
  }
}
