package app.bpartners.api.unit.validator;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.api.endpoint.rest.model.ConverterAnnotation;
import app.bpartners.api.endpoint.rest.model.ConverterAnnotationRegion;
import app.bpartners.api.endpoint.rest.model.ConverterAnnotationShapeAttributes;
import app.bpartners.api.endpoint.rest.validator.ConverterValidator;
import app.bpartners.api.model.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConverterValidatorTest {
  private static final ConverterValidator subject = new ConverterValidator();

  private static ConverterAnnotation validAnnotation() {
    var region =
        new ConverterAnnotationRegion()
            .shapeAttributes(
                new ConverterAnnotationShapeAttributes()
                    .allPointsX(
                        List.of(
                            BigDecimal.valueOf(1.0),
                            BigDecimal.valueOf(2.0),
                            BigDecimal.valueOf(3.0)))
                    .allPointsY(
                        List.of(
                            BigDecimal.valueOf(4.0),
                            BigDecimal.valueOf(5.0),
                            BigDecimal.valueOf(6.0))));

    return new ConverterAnnotation().regions(Map.of("key", region));
  }

  @Test
  void valid_single_region_and_polygon_should_pass() {
    var input = Map.of("annotation1", validAnnotation());
    assertDoesNotThrow(() -> subject.accept(input));
  }

  @Test
  void multiple_polygons_should_throw() {
    var input = Map.of("a1", validAnnotation(), "a2", validAnnotation());
    var ex = assertThrows(BadRequestException.class, () -> subject.accept(input));
    assertTrue(ex.getMessage().contains("Only one PolygonAnnotation"));
  }

  @Test
  void multiple_regions_should_throw() {
    var region =
        new ConverterAnnotationRegion()
            .shapeAttributes(
                new ConverterAnnotationShapeAttributes()
                    .allPointsX(
                        List.of(
                            BigDecimal.valueOf(1.0),
                            BigDecimal.valueOf(2.0),
                            BigDecimal.valueOf(3.0)))
                    .allPointsY(
                        List.of(
                            BigDecimal.valueOf(4.0),
                            BigDecimal.valueOf(5.0),
                            BigDecimal.valueOf(6.0))));

    var converter = new ConverterAnnotation().regions(Map.of("r1", region, "r2", region));

    var input = Map.of("single", converter);
    var ex = assertThrows(BadRequestException.class, () -> subject.accept(input));
    assertTrue(ex.getMessage().contains("Only one RegionAnnotation"));
  }

  @Test
  void mismatch_between_X_and_Y_should_throw() {
    var region =
        new ConverterAnnotationRegion()
            .shapeAttributes(
                new ConverterAnnotationShapeAttributes()
                    .allPointsX(List.of(BigDecimal.valueOf(1.0), BigDecimal.valueOf(2.0)))
                    .allPointsY(List.of(BigDecimal.valueOf(1.0))));

    var converter = new ConverterAnnotation().regions(Map.of("r1", region));

    var input = Map.of("only", converter);

    var ex = assertThrows(BadRequestException.class, () -> subject.accept(input));
    assertTrue(ex.getMessage().contains("Mismatch between X and Y coordinates"));
  }
}
