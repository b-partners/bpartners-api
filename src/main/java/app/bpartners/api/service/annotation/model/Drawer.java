package app.bpartners.api.service.annotation.model;

import app.bpartners.api.model.annotation.IntXY;
import app.bpartners.api.service.annotation.factory.RoofSlopeBoundaryFactory;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import lombok.Builder;

public class Drawer {
  private Drawer() {}

  public static void drawFillPolygon(Graphics2D g2d, Color color, Coordinates polygon) {
    g2d.setColor(color);
    g2d.fillPolygon(polygon.allX(), polygon.allY(), polygon.allX().length);
  }

  public static void drawStrokePolygon(
      Graphics2D g2d,
      Transform transform,
      ExportAreaPictureAnnotation3DPan pan,
      float strokeWidthMultiplier) {
    List<RoofSlopBoundary> boundaries = RoofSlopeBoundaryFactory.create(transform, pan);
    boundaries.forEach(
        boundary -> {
          var stroke = (BasicStroke) boundary.getType().getStroke();
          var scaledStroke =
              new BasicStroke(
                  stroke.getLineWidth() * strokeWidthMultiplier,
                  stroke.getEndCap(),
                  stroke.getLineJoin(),
                  stroke.getMiterLimit(),
                  stroke.getDashArray(),
                  stroke.getDashPhase());

          drawStrokePolygon(
              g2d, boundary.getType().getColor(), scaledStroke, boundary.getCoordinates());
        });
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

  public static BufferedImage createStrokeIllustration(RoofSlopeBoundaryType type) {
    int width = 300;
    int height = 80;

    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    Graphics2D g2d = image.createGraphics();

    try {
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      // background
      g2d.setColor(new Color(255, 241, 234));
      g2d.fillRect(0, 0, width, height);

      // stroke line
      var stroke = (BasicStroke) type.getStroke();
      g2d.setColor(type.getColor());
      g2d.setStroke(
          new BasicStroke(
              9f,
              stroke.getEndCap(),
              stroke.getLineJoin(),
              stroke.getMiterLimit(),
              stroke.getDashArray(),
              stroke.getDashPhase()));

      int y = height / 2;

      g2d.drawLine(30, y, width - 30, y);
    } finally {
      g2d.dispose();
    }

    return image;
  }

  public static void drawPolygonMeasurements(
      Graphics2D g2d,
      MeasurementConf conf,
      Coordinates polygon,
      List<ExportAreaPictureAnnotationMeasurement> measurements,
      int imageWidth,
      int imageHeight) {

    float targetFontSize = conf.font().getSize2D();
    java.awt.Font largerFont = conf.font().deriveFont(targetFontSize);
    g2d.setFont(largerFont);

    int numPoints = Math.min(polygon.allX().length - 1, measurements.size());

    for (int i = 0; i < numPoints; i++) {
      var measurement = measurements.get(i);
      if (!measurement.getIsInvisible()) {
        var measurementText = measurement.getValue() + measurement.getUnit();
        var fontMetrics = g2d.getFontMetrics(largerFont);

        int textWidth = fontMetrics.stringWidth(measurementText);
        int textHeight = fontMetrics.getHeight();
        int ascent = fontMetrics.getAscent();

        var point1 = new IntXY(polygon.allX()[i], polygon.allY()[i]);
        var point2 = new IntXY(polygon.allX()[i + 1], polygon.allY()[i + 1]);

        double midX = (point1.x() + point2.x()) / 2.0;
        double midY = (point1.y() + point2.y()) / 2.0;

        double dx = point2.x() - (double) point1.x();
        double dy = point2.y() - (double) point1.y();
        double len = Math.hypot(dx, dy);

        if (len == 0) continue;

        double nx = -dy / len;
        double ny = dx / len;

        double pushDistance = 25.0;

        double targetCenterX = midX + nx * pushDistance;
        double targetCenterY = midY + ny * pushDistance;

        int paddingX = conf.offset.x() > 0 ? conf.offset.x() : 8;
        int paddingY = conf.offset.y() > 0 ? conf.offset.y() : 6;

        int boxWidth = textWidth + (paddingX * 2);
        int boxHeight = textHeight + (paddingY * 2);

        int boxX = (int) (targetCenterX - boxWidth / 2.0);
        int boxY = (int) (targetCenterY - boxHeight / 2.0);

        boxX = Math.clamp(boxX, 0, imageWidth - boxWidth);
        boxY = Math.clamp(boxY, 0, imageHeight - boxHeight);

        g2d.setColor(conf.bgColor());
        g2d.fillRect(boxX, boxY, boxWidth, boxHeight);

        g2d.setColor(conf.textColor());
        int textX = boxX + paddingX;
        int textY = boxY + paddingY + ascent;

        g2d.drawString(measurementText, textX, textY);
      }
    }
  }

  @Builder
  public record MeasurementConf(IntXY offset, Color textColor, Color bgColor, Font font) {}
}
