package app.bpartners.api.service.annotation.model;

import static java.util.Objects.requireNonNull;

import app.bpartners.api.service.annotation.Polygon;

public record Coordinates(int[] allX, int[] allY) {
  public static Coordinates from(Polygon polygon) {
    if (polygon == null) {
      return new Coordinates(new int[0], new int[0]);
    }
    var points = requireNonNull(polygon.points());
    var allX = new int[points.size()];
    var allY = new int[points.size()];

    for (int i = 0; i < points.size(); i++) {
      var coordinate = points.get(i);
      allX[i] = (int) Math.round(coordinate.x());
      allY[i] = (int) Math.round(coordinate.y());
    }

    return new Coordinates(allX, allY);
  }
}
