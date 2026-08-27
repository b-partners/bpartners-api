package app.bpartners.api.unit.service.annotation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import app.bpartners.api.endpoint.rest.model.Point;
import app.bpartners.api.endpoint.rest.model.Polygon;
import app.bpartners.api.service.annotation.model.Coordinates;
import java.util.List;
import org.junit.jupiter.api.Test;

class CoordinatesTest {
  @Test
  void from_polygon_extracts_x_and_y_arrays() {
    var polygon =
        new Polygon().points(List.of(new Point().x(1d).y(2d), new Point().x(3d).y(4d)));

    var actual = Coordinates.from(polygon);

    assertEquals(List.of(1, 3), boxed(actual.allX()));
    assertEquals(List.of(2, 4), boxed(actual.allY()));
  }

  @Test
  void equals_and_hash_code_consider_array_content() {
    var polygon =
        new Polygon().points(List.of(new Point().x(1d).y(2d), new Point().x(3d).y(4d)));

    var first = Coordinates.from(polygon);
    var second = Coordinates.from(polygon);

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
    assertNotEquals(first, Coordinates.from(new Polygon().points(List.of(new Point().x(9d).y(9d)))));
  }

  @Test
  void to_string_includes_array_content() {
    var polygon = new Polygon().points(List.of(new Point().x(1d).y(2d)));

    var actual = Coordinates.from(polygon).toString();

    assertEquals("Coordinates[allX=[1], allY=[2]]", actual);
  }

  private static List<Integer> boxed(int[] values) {
    return java.util.stream.IntStream.of(values).boxed().toList();
  }
}
