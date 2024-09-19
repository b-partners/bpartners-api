package app.bpartners.api.unit.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.api.model.AreaPictureAnnotationInstance;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.validator.PolygonValidator;
import org.junit.jupiter.api.Test;

class PolygonValidatorTest {
  PolygonValidator subject = new PolygonValidator();

  @Test
  void subject_throws_bad_request_exception() {
    var polygon = AreaPictureAnnotationInstance.Polygon.builder().build();

    var actual =
        assertThrows(
            BadRequestException.class,
            () -> {
              subject.accept(polygon);
            });

    var expected = "polygon points attribute is mandatory";
    assertEquals(expected, actual.getMessage());
  }
}
