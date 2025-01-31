package app.bpartners.api.service.annotation;

import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.Font.PLAIN;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.util.Objects.requireNonNull;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstance;
import app.bpartners.api.model.exception.BadRequestException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class ExportAreaPictureAnnotationImageGenerator {
  private static final int IMAGE_SCALE = 2;
  private static final int POINT_SIZE = 6;
  private static final int MEASUREMENT_PADDING_X = 6;
  private static final int MEASUREMENT_PADDING_Y = 4;
  private static final float STROKE_SIZE = 1.2f;
  private static final Font MEASUREMENT_FONT = new Font("Arial", PLAIN, 11);
  private static final Color MEASUREMENT_BG_COLOR = new Color(0, 0, 0, 150);

  public BufferedImage apply(ExportAreaPictureAnnotation annotation) {
    try {
      BufferedImage image = downloadAnnotationImageWithScale(annotation.getImageUrl());
      return drawAnnotations(image, annotation);
    } catch (IOException | URISyntaxException e) {
      throw new BadRequestException("Cannot read the image from the url");
    }
  }

  public static BufferedImage downloadAnnotationImageWithScale(String imageUrl)
      throws URISyntaxException, IOException {
    BufferedImage image = ImageIO.read(new URI(imageUrl).toURL());
    int newWidth = image.getWidth() * IMAGE_SCALE;
    int newHeight = image.getHeight() * IMAGE_SCALE;
    var resizedImage = new BufferedImage(newWidth, newHeight, image.getType());

    Graphics2D graphics2D = resizedImage.createGraphics();
    graphics2D.drawImage(image, 0, 0, newWidth, newHeight, null);
    graphics2D.dispose();
    return resizedImage;
  }

  private static BufferedImage drawAnnotations(
      BufferedImage image, ExportAreaPictureAnnotation annotation) {
    Graphics2D graphics2D = image.createGraphics();
    graphics2D.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
    graphics2D.setFont(MEASUREMENT_FONT);
    graphics2D.setStroke(new BasicStroke(STROKE_SIZE));

    var annotationInstances = annotation.getAnnotations();
    annotationInstances.forEach(
        annotationInstance -> drawAnnotationInstance(graphics2D, annotationInstance));
    return image;
  }

  private static void drawAnnotationInstance(
      Graphics2D graphics2D, ExportAreaPictureAnnotationInstance annotationInstance) {
    var points = annotationInstance.getPolygon().getPoints();
    var pointSize = requireNonNull(points).size();
    int[] xPoints =
        points.stream()
            .mapToInt(point -> requireNonNull(point.getX()).intValue() * IMAGE_SCALE)
            .toArray();
    int[] yPoints =
        points.stream()
            .mapToInt(point -> requireNonNull(point.getY()).intValue() * IMAGE_SCALE)
            .toArray();

    // Draw polygon strokeLine
    graphics2D.setColor(hexToColor(annotationInstance.getStrokeColor()));
    graphics2D.drawPolygon(xPoints, yPoints, pointSize);

    // Fill the polygon
    graphics2D.setColor(hexToColor(annotationInstance.getFillColor()));
    graphics2D.fillPolygon(xPoints, yPoints, pointSize);

    // Draw all points
    graphics2D.setColor(BLACK);
    for (int i = 0; i < pointSize; i++) {
      int x = xPoints[i];
      int y = yPoints[i];
      graphics2D.fillOval(x - POINT_SIZE / 2, y - POINT_SIZE / 2, POINT_SIZE, POINT_SIZE);
    }

    drawMeasurements(graphics2D, annotationInstance, xPoints, yPoints);
  }

  private static void drawMeasurements(
      Graphics2D graphics2D,
      ExportAreaPictureAnnotationInstance annotationInstance,
      int[] xPoints,
      int[] yPoints) {
    var measurements = annotationInstance.getMeasurements();
    for (int i = 0; i < xPoints.length - 1; i++) {
      var measurement = measurements.get(i);
      if (measurement.getIsInvisible()) continue;

      String measurementText = measurement.getValue() + measurement.getUnit();
      FontMetrics fontMetrics = graphics2D.getFontMetrics(MEASUREMENT_FONT);
      int textWidth = fontMetrics.stringWidth(measurementText);
      int textHeight = fontMetrics.getHeight();
      int x1 = xPoints[i];
      int x2 = xPoints[i + 1];
      int y1 = yPoints[i];
      int y2 = yPoints[i + 1];
      int measurementX = (x1 + x2) / 2 - (textWidth / 2);
      int measurementY = (y1 + y2) / 2 - (textHeight / 2);

      graphics2D.setColor(MEASUREMENT_BG_COLOR);
      graphics2D.fillRect(
          measurementX - MEASUREMENT_PADDING_X / 2,
          measurementY - MEASUREMENT_PADDING_Y / 2,
          textWidth + MEASUREMENT_PADDING_X,
          textHeight + MEASUREMENT_PADDING_Y);

      graphics2D.setColor(WHITE);
      graphics2D.drawString(
          measurementText, measurementX, measurementY + MEASUREMENT_PADDING_Y + textHeight / 2);
    }
  }

  private static Color hexToColor(String hexColor) {
    if (!hexColor.startsWith("#") || (hexColor.length() != 7 && hexColor.length() != 9)) {
      throw new BadRequestException("Wrong color format was received");
    }
    hexColor = hexColor.substring(1);
    int red = Integer.valueOf(hexColor.substring(0, 2), 16);
    int green = Integer.valueOf(hexColor.substring(2, 4), 16);
    int blue = Integer.valueOf(hexColor.substring(4, 6), 16);
    int alpha = hexColor.length() == 8 ? Integer.valueOf(hexColor.substring(6, 8), 16) : 255;
    return new Color(red, green, blue, alpha);
  }
}
