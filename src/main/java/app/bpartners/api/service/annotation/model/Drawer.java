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
    Shape clip = g2d.getClip();
    Rectangle bounds = clip != null ? clip.getBounds() : null;

    g2d.setFont(conf.font());

    for (int i = 0; i < polygon.allX().length - 1; i++) {
      if (measurements.size() <= i) {
        continue;
      }

      var measurement = measurements.get(i);
      if (measurement.getIsInvisible()) {
        continue;
      }

      String measurementText = measurement.getValue() + measurement.getUnit();
      FontMetrics fontMetrics = g2d.getFontMetrics();
      int textWidth = fontMetrics.stringWidth(measurementText);
      int textHeight = fontMetrics.getHeight();

      int boxWidth = textWidth + conf.offset().x();
      int boxHeight = textHeight + conf.offset().y();

      int rectX = (polygon.allX()[i] + polygon.allX()[i + 1]) / 2 - boxWidth / 2;
      int rectY = (polygon.allY()[i] + polygon.allY()[i + 1]) / 2 - boxHeight / 2;

      // Clamp to clip boundaries if available
      if (bounds != null) {
        rectX = Math.max(bounds.x, Math.min(rectX, bounds.x + bounds.width - boxWidth));
        rectY = Math.max(bounds.y, Math.min(rectY, bounds.y + bounds.height - boxHeight));
      }

      g2d.setColor(conf.bgColor());
      g2d.fillRect(rectX, rectY, boxWidth, boxHeight);

      g2d.setColor(conf.textColor);
      int textX = rectX + conf.offset().x() / 2;
      int textY = rectY + conf.offset().y() / 2 + fontMetrics.getAscent();
      g2d.drawString(measurementText, textX, textY);
    }
  }

  @Builder
  public record MeasurementConf(IntXY offset, Color textColor, Color bgColor, Font font) {}
}
