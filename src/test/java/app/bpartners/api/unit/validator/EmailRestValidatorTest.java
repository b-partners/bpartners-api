package app.bpartners.api.unit.validator;

import static app.bpartners.api.endpoint.rest.model.EmailStatus.DRAFT;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.api.endpoint.rest.model.CreateEmail;
import app.bpartners.api.endpoint.rest.validator.EmailRestValidator;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.List;
import org.junit.jupiter.api.Test;

class EmailRestValidatorTest {
  EmailRestValidator subject = new EmailRestValidator();

  @Test
  void create_email_with_empty_attributes_ko() {
    var actual = assertThrows(BadRequestException.class, () -> subject.accept(new CreateEmail()));

    assertEquals(
        "Attribute `id` is mandatory. "
            + "Attribute `emailObject` is mandatory. "
            + "Attribute `emailBody` is mandatory. "
            + "Attribute `status` is mandatory. "
            + "Attribute `recipients` is mandatory. ",
        actual.getMessage());
  }

  @Test
  void create_email_has_malformed_or_not_allowed_tags_ko() {
    var actual =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.accept(
                    new CreateEmail()
                        .id(randomUUID().toString())
                        .emailObject("dummy")
                        .emailBody("<spn></span>")
                        .status(DRAFT)
                        .recipients(List.of("dummy"))));

    assertEquals(
        "Your HTML syntax is malformed or you use other tags than these allowed : a, b, blockquote,"
            + " br, caption, cite, code, col, colgroup, dd, div, dl, dt, em, h1, h2, h3, h4, h5,"
            + " h6, i, img, li, ol, p, pre, q, small, span, strike, strong, sub, sup, table, tbody,"
            + " td, tfoot, th, thead, tr, u, ul, del",
        actual.getMessage());
  }

  @Test
  void create_email_with_correct_attributes_ok() {
    assertDoesNotThrow(
        () ->
            subject.accept(
                new CreateEmail()
                    .id(randomUUID().toString())
                    .emailObject("dummy")
                    .emailBody("<span></span>")
                    .status(DRAFT)
                    .recipients(List.of("dummy"))));
  }
}
