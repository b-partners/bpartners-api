package app.bpartners.api.unit.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.api.model.AreaPictureAnnotationInstance;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.validator.AreaPictureAnnotationInstanceValidator;
import org.junit.jupiter.api.Test;

class AreaPictureAnnotationInstanceValidatortTest {
  AreaPictureAnnotationInstanceValidator subject = new AreaPictureAnnotationInstanceValidator();

  @Test
  void subjectThrowBadRequestException() {
    var annotation = AreaPictureAnnotationInstance.builder().build();

    var actual =
        assertThrows(
            BadRequestException.class,
            () -> {
              subject.accept(annotation);
            });

    var expected =
        "id is mandatorylabelName is mandatoryidUser is mandatoryidAreaPicture is"
            + " mandatoryidAnnotation is mandatorypolygon is mandatory";
    assertEquals(expected, actual.getMessage());
  }
}
