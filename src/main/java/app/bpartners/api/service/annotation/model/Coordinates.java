package app.bpartners.api.service.annotation.model;

import static java.util.Objects.requireNonNull;

import app.bpartners.api.endpoint.rest.model.Polygon;
import java.util.Arrays;

public record Coordinates(int[] allX, int[] allY) {
  public static Coordinates from(Polygon polygon) {
    var coordinates = requireNonNull(polygon.getPoints());
    var allX = new int[coordinates.size()];
    var allY = new int[coordinates.size()];

    for (int i = 0; i < coordinates.size(); i++) {
      var coordinate = coordinates.get(i);
      allX[i] = requireNonNull(coordinate.getX()).intValue();
      allY[i] = requireNonNull(coordinate.getY()).intValue();
    }

    return new Coordinates(allX, allY);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Coordinates other)) {
      return false;
    }
    return Arrays.equals(allX, other.allX) && Arrays.equals(allY, other.allY);
  }

  @Override
  public int hashCode() {
    return 31 * Arrays.hashCode(allX) + Arrays.hashCode(allY);
  }

  @Override
  public String toString() {
    return "Coordinates[allX=" + Arrays.toString(allX) + ", allY=" + Arrays.toString(allY) + "]";
  }
}
