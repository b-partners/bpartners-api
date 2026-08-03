package app.bpartners.api.service.annotation;

public record Point(double x, double y) {
  public Point withX(double x) {
    return new Point(x, this.y);
  }

  public Point withY(double y) {
    return new Point(this.x, y);
  }
}
