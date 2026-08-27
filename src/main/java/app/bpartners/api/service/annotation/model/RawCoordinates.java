package app.bpartners.api.service.annotation.model;

import static java.util.Objects.requireNonNull;

import app.bpartners.api.endpoint.rest.model.Polygon;

/**
 * Polygon coordinates in their original unit (e.g. meters), kept as doubles so that sub-unit
 * precision survives until {@link Transform#apply(RawCoordinates)} rounds them to pixels.
 * Truncating to {@code int} here would collapse distinct vertices onto the same point whenever the
 * polygon spans only a few units.
 */
public record RawCoordinates(double[] allX, double[] allY) {
  public static RawCoordinates from(Polygon polygon) {
    var coordinates = requireNonNull(polygon.getPoints());
    var allX = new double[coordinates.size()];
    var allY = new double[coordinates.size()];

    for (int i = 0; i < coordinates.size(); i++) {
      var coordinate = coordinates.get(i);
      allX[i] = requireNonNull(coordinate.getX()).doubleValue();
      allY[i] = requireNonNull(coordinate.getY()).doubleValue();
    }

    return new RawCoordinates(allX, allY);
  }
}
