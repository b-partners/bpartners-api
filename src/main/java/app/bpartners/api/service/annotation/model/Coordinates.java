package app.bpartners.api.service.annotation.model;

import static java.util.Objects.requireNonNull;

import app.bpartners.api.endpoint.rest.model.Polygon;

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
}
