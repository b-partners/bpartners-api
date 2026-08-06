package app.bpartners.api.service.annotation;

import java.util.ArrayList;
import java.util.List;

public record Polygon(List<Point> points) {
  public Polygon {
    points = points == null ? List.of() : List.copyOf(points);
  }

  public Polygon scale(double scaleX, double scaleY) {
    var scaledPoints = new ArrayList<Point>();
    for (var p : points) {
      scaledPoints.add(new Point(p.x() * scaleX, p.y() * scaleY));
    }
    return new Polygon(scaledPoints);
  }
}
