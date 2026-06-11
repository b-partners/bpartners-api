package app.bpartners.api.service.annotation;

import static java.awt.Color.BLACK;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.util.Objects.requireNonNull;

import app.bpartners.api.model.annotation.IntXY;
import app.bpartners.api.service.annotation.factory.ColorFactory;
import app.bpartners.api.service.annotation.model.ExportAreaPictureAnnotationInstance;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import org.apache.commons.lang3.function.TriFunction;
import org.springframework.stereotype.Component;

@Component
public class ExportAreaPictureAnnotationImageGenerator
    implements TriFunction<
        BufferedImage,
        ExportAreaPictureAnnotationImageConf,
        List<ExportAreaPictureAnnotationInstance>,
        BufferedImage> {

  @Override
  public BufferedImage apply(
      BufferedImage image,
      ExportAreaPictureAnnotationImageConf conf,
      List<ExportAreaPictureAnnotationInstance> annotations) {
    BufferedImage scaledImage = scaleImage(image, conf);
    return drawAnnotations(scaledImage, conf, annotations);
  }

  private BufferedImage scaleImage(BufferedImage image, ExportAreaPictureAnnotationImageConf conf) {
    int newWidth = image.getWidth() * conf.getScale();
    int newHeight = image.getHeight() * conf.getScale();
    var resizedImage = new BufferedImage(newWidth, newHeight, image.getType());

    Graphics2D graphics2D = resizedImage.createGraphics();
    graphics2D.drawImage(image, 0, 0, newWidth, newHeight, null);
    graphics2D.dispose();
    return resizedImage;
  }

  private BufferedImage drawAnnotations(
      BufferedImage image,
      ExportAreaPictureAnnotationImageConf conf,
      List<ExportAreaPictureAnnotationInstance> annotations) {
    Graphics2D graphics2D = image.createGraphics();
    graphics2D.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
    graphics2D.setFont(conf.getMeasurementFont());
    graphics2D.setStroke(conf.getStroke());

    annotations.forEach(
        annotationInstance ->
            drawAnnotationInstance(
                graphics2D, image.getWidth(), image.getHeight(), conf, annotationInstance));
    graphics2D.dispose();
    return image;
  }

  private void drawAnnotationInstance(
      Graphics2D graphics2D,
      int imageWidth,
      int imageHeight,
      ExportAreaPictureAnnotationImageConf conf,
      ExportAreaPictureAnnotationInstance annotationInstance) {
    var points = annotationInstance.getPolygon().getPoints();
    List<IntXY> coordinates =
        requireNonNull(points).stream()
            .map(
                point ->
                    new IntXY(
                        requireNonNull(point.getX()).intValue() * conf.getScale(),
                        requireNonNull(point.getY()).intValue() * conf.getScale()))
            .toList();
    int[] xPoints = coordinates.stream().mapToInt(IntXY::x).toArray();
    int[] yPoints = coordinates.stream().mapToInt(IntXY::y).toArray();

    // Draw polygon strokeLine
    graphics2D.setColor(ColorFactory.make(annotationInstance.getStrokeColor()));
    graphics2D.drawPolygon(xPoints, yPoints, coordinates.size());

    // Fill the polygon
    graphics2D.setColor(ColorFactory.make(annotationInstance.getFillColor()));
    graphics2D.fillPolygon(xPoints, yPoints, coordinates.size());

    // Draw all points
    graphics2D.setColor(BLACK);
    coordinates.forEach(
        coordinate -> {
          graphics2D.fillOval(
              coordinate.x() - conf.getPointSize() / 2,
              coordinate.y() - conf.getPointSize() / 2,
              conf.getPointSize(),
              conf.getPointSize());
        });

    drawMeasurements(graphics2D, imageWidth, imageHeight, conf, annotationInstance, coordinates);
  }

  private void drawMeasurements(
      Graphics2D graphics2D,
      int imageWidth,
      int imageHeight,
      ExportAreaPictureAnnotationImageConf conf,
      ExportAreaPictureAnnotationInstance annotationInstance,
      List<IntXY> coordinates) {
    var measurements = annotationInstance.getMeasurements();
    for (int i = 0; i < coordinates.size() - 1; i++) {
      var measurement = measurements.get(i);
      if (measurement.getIsInvisible()) {
        continue;
      }

      String measurementText = measurement.getValue() + measurement.getUnit();
      FontMetrics fontMetrics = graphics2D.getFontMetrics(conf.getMeasurementFont());
      int textWidth = fontMetrics.stringWidth(measurementText);
      int textHeight = fontMetrics.getHeight();

      int boxWidth = textWidth + conf.getMeasurementOffset().x();
      int boxHeight = textHeight + conf.getMeasurementOffset().y();

      IntXY point1 = coordinates.get(i);
      IntXY point2 = coordinates.get(i + 1);

      int rectX = (point1.x() + point2.x()) / 2 - boxWidth / 2;
      int rectY = (point1.y() + point2.y()) / 2 - boxHeight / 2;

      // Clamp to image boundaries
      rectX = Math.clamp(rectX, 0, imageWidth - boxWidth);
      rectY = Math.clamp(rectY, 0, imageHeight - boxHeight);

      graphics2D.setColor(conf.getMeasurementBgColor());
      graphics2D.fillRect(rectX, rectY, boxWidth, boxHeight);

      graphics2D.setColor(conf.getMeasurementTextColor());
      int textX = rectX + conf.getMeasurementOffset().x() / 2;
      int textY = rectY + conf.getMeasurementOffset().y() / 2 + fontMetrics.getAscent();
      graphics2D.drawString(measurementText, textX, textY);
    }
  }
}
