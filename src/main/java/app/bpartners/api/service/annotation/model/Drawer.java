package app.bpartners.api.service.annotation.model;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationMeasurement;
import app.bpartners.api.model.annotation.IntXY;
import java.awt.*;
import java.util.List;
import lombok.Builder;

public class Drawer {
  private Drawer() {}

  public static void drawFillPolygon(Graphics2D g2d, Color color, Coordinates polygon) {
    g2d.setColor(color);
    g2d.fillPolygon(polygon.allX(), polygon.allY(), polygon.allX().length);
  }

  public static void drawStrokePolygon(
      Graphics2D g2d, Color color, Stroke stroke, Coordinates polygon) {
    g2d.setColor(color);
    g2d.setStroke(stroke);
    g2d.drawPolygon(polygon.allX(), polygon.allY(), polygon.allX().length);
  }

  public static void drawPolygonPoints(Graphics2D g2d, Color color, int size, Coordinates polygon) {
    g2d.setColor(color);
    for (int i = 0; i < polygon.allX().length; i++) {
      var x = polygon.allX()[i];
      var y = polygon.allY()[i];
      g2d.fillOval(x - size / 2, y - size / 2, size, size);
    }
  }

  public static void drawPolygonMeasurements(
      Graphics2D g2d,
      MeasurementConf conf,
      Coordinates polygon,
      List<ExportAreaPictureAnnotationMeasurement> measurements) {
    g2d.setFont(conf.font());

    for (int i = 0; i < polygon.allX().length - 1; i++) {
      if (measurements.size() <= i) {
        continue;
      }

      var measurement = measurements.get(i);
      if (measurement.getIsInvisible()) {
        continue;
      }

      var measurementText = measurement.getValue() + measurement.getUnit();
      var fontMetrics = g2d.getFontMetrics();
      int textWidth = fontMetrics.stringWidth(measurementText);
      int textHeight = fontMetrics.getHeight();
      var point1 = new IntXY(polygon.allX()[i], polygon.allY()[i]);
      var point2 = new IntXY(polygon.allX()[i + 1], polygon.allY()[i + 1]);
      var measurementCoordinate =
          new IntXY(
              (point1.x() + point2.x()) / 2 - (textWidth / 2),
              (point1.y() + point2.y()) / 2 - (textHeight / 2));

      g2d.setColor(conf.bgColor());
      g2d.fillRect(
          measurementCoordinate.x() - conf.offset.x() / 2,
          measurementCoordinate.y() - conf.offset.y() / 2,
          textWidth + conf.offset.x(),
          textHeight + conf.offset.y());

      g2d.setColor(conf.textColor);
      g2d.drawString(
          measurementText,
          measurementCoordinate.x(),
          measurementCoordinate.y() + conf.offset.y() + textHeight / 2);
    }
  }

  @Builder
  public record MeasurementConf(IntXY offset, Color textColor, Color bgColor, Font font) {}
}
