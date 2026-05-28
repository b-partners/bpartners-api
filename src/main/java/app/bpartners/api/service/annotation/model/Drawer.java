package app.bpartners.api.service.annotation.model;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationMeasurement;
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
      g2d.setColor(type.getColor());
      g2d.setStroke(type.getStroke());

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

    // 1. Make the text bigger by deriving a larger font from your configuration
    float targetFontSize =
        conf.font().getSize2D() * 1.5f; // Adjust the 1.5f multiplier to make it bigger/smaller
    java.awt.Font largerFont = conf.font().deriveFont(targetFontSize);
    g2d.setFont(largerFont);

    // Fallback to avoid division by zero or errors if list size mismatches
    int numPoints = Math.min(polygon.allX().length - 1, measurements.size());

    for (int i = 0; i < numPoints; i++) {
      var measurement = measurements.get(i);
      if (measurement.getIsInvisible()) {
        continue;
      }

      var measurementText = measurement.getValue() + measurement.getUnit();
      var fontMetrics = g2d.getFontMetrics(largerFont);

      // Accurately measure text based on the new larger font size
      int textWidth = fontMetrics.stringWidth(measurementText);
      int textHeight = fontMetrics.getHeight();
      int ascent = fontMetrics.getAscent();

      var point1 = new IntXY(polygon.allX()[i], polygon.allY()[i]);
      var point2 = new IntXY(polygon.allX()[i + 1], polygon.allY()[i + 1]);

      // Calculate midpoint of the current segment
      double midX = (point1.x() + point2.x()) / 2.0;
      double midY = (point1.y() + point2.y()) / 2.0;

      // --- Calculate the outward perpendicular vector ---
      // Edge vector
      double dx = point2.x() - point1.x();
      double dy = point2.y() - point1.y();
      double len = Math.hypot(dx, dy);

      if (len == 0) continue; // Avoid division by zero for identical vertices

      // Normal vector pointing to one side (perpendicular)
      double nx = -dy / len;
      double ny = dx / len;

      // Push distance outside the line (adjust this value if you want it further away)
      double pushDistance = 25.0;

      // Assuming a standard clockwise/counter-clockwise ordered polygon,
      // we choose the direction pointing OUTWARD. If it pushes inside, flip the sign.
      // Let's bias it outward. (If it pushes inside for your specific polygon winding, change to -
      // nx and - ny)
      double targetCenterX = midX + nx * pushDistance;
      double targetCenterY = midY + ny * pushDistance;

      // Define background box padding
      int paddingX = conf.offset.x() > 0 ? conf.offset.x() : 8;
      int paddingY = conf.offset.y() > 0 ? conf.offset.y() : 6;

      int boxWidth = textWidth + (paddingX * 2);
      int boxHeight = textHeight + (paddingY * 2);

      // Calculate top-left of the background rectangle based on the pushed center
      int boxX = (int) (targetCenterX - boxWidth / 2.0);
      int boxY = (int) (targetCenterY - boxHeight / 2.0);

      // Prevent overflow
      boxX = Math.max(0, Math.min(boxX, imageWidth  - boxWidth));
      boxY = Math.max(0, Math.min(boxY, imageHeight - boxHeight));

      // 2. Draw background rectangle
      g2d.setColor(conf.bgColor());
      g2d.fillRect(boxX, boxY, boxWidth, boxHeight);

      // 3. Draw the crisp text centered inside the background rectangle
      g2d.setColor(conf.textColor());
      int textX = boxX + paddingX;
      int textY = boxY + paddingY + ascent; // baseline alignment

      g2d.drawString(measurementText, textX, textY);
    }
  }

  @Builder
  public record MeasurementConf(IntXY offset, Color textColor, Color bgColor, Font font) {}
}
