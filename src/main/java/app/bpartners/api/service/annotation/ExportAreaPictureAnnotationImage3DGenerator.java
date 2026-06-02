package app.bpartners.api.service.annotation;

import static app.bpartners.api.service.annotation.model.Drawer.*;
import static java.awt.Color.*;
import static java.awt.Font.PLAIN;
import static java.util.Objects.requireNonNull;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.model.annotation.IntXY;
import app.bpartners.api.service.annotation.factory.BufferedImageFactory;
import app.bpartners.api.service.annotation.factory.Graphics2DFactory;
import app.bpartners.api.service.annotation.model.Coordinates;
import app.bpartners.api.service.annotation.model.Pair;
import app.bpartners.api.service.annotation.model.Transform;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ExportAreaPictureAnnotationImage3DGenerator {
  private static final int TARGET_SIZE = 550;
  private static final int CONTENT_SIZE = 500;

  private static final int POLYGON_POINTS_SIZE = 10;
  private static final Stroke POLYGON_STROKE = new BasicStroke(3f);
  private static final Color SELECTED_PAN_COLOR = new Color(229, 142, 25);
  private static final MeasurementConf MEASUREMENT_CONF =
      MeasurementConf.builder()
          .offset(new IntXY(0, 6))
          .textColor(BLACK)
          .bgColor(new Color(200, 200, 200, 150))
          .font(new Font("Arial", PLAIN, 35)) // TODO: sync with pdf font
          .build();

  public Pair<Transform, BufferedImage> generateBaseImage(
      List<ExportAreaPictureAnnotation3DPan> pans) {
    var allPoints =
        pans.stream()
            .flatMap(pan -> requireNonNull(pan.getPolygon().getPoints()).stream())
            .map(
                p ->
                    new IntXY(
                        requireNonNull(p.getX()).intValue(), requireNonNull(p.getY()).intValue()))
            .toList();

    var allX = allPoints.stream().mapToInt(IntXY::x).toArray();
    var allY = allPoints.stream().mapToInt(IntXY::y).toArray();
    var transform = Transform.from(new Coordinates(allX, allY), CONTENT_SIZE, TARGET_SIZE);

    var baseImage = BufferedImageFactory.make(TARGET_SIZE, TARGET_SIZE);
    var g2d = Graphics2DFactory.make(baseImage);

    pans.forEach(
        pan -> {
          var rawData = Coordinates.from(requireNonNull(pan.getPolygon()));
          var mapped = transform.apply(rawData);

          drawFillPolygon(g2d, RED, mapped);
          drawStrokePolygon(g2d, WHITE, POLYGON_STROKE, mapped);
        });

    g2d.dispose();

    return new Pair<>(transform, baseImage);
  }

  public Pair<Transform, BufferedImage> generateBaseImageWithSlopeBoundary(
      List<ExportAreaPictureAnnotation3DPan> pans) {
    var allPoints =
        pans.stream()
            .flatMap(pan -> requireNonNull(pan.getPolygon().getPoints()).stream())
            .map(
                p ->
                    new IntXY(
                        requireNonNull(p.getX()).intValue(), requireNonNull(p.getY()).intValue()))
            .toList();

    var allX = allPoints.stream().mapToInt(IntXY::x).toArray();
    var allY = allPoints.stream().mapToInt(IntXY::y).toArray();
    var transform = Transform.from(new Coordinates(allX, allY), CONTENT_SIZE, TARGET_SIZE);

    var baseImage = BufferedImageFactory.make(TARGET_SIZE, TARGET_SIZE);
    var g2d = Graphics2DFactory.make(baseImage);

    pans.forEach(pan -> drawStrokePolygon(g2d, transform, pan, 1f));

    g2d.dispose();

    return new Pair<>(transform, baseImage);
  }

  public BufferedImage generatePanImage(
      BufferedImage baseImage, Transform transform, ExportAreaPictureAnnotation3DPan pan) {
    var panImage = generateBaseImageWithHighlightedPan(baseImage, transform, pan);
    var panImageWithMeasurements = generatePanImageWithMeasurements(pan);

    return mergePanImagesSideBySide(panImage, panImageWithMeasurements);
  }

  public BufferedImage generateBaseImageWithHighlightedPanWithSlopeBoundary(
      BufferedImage baseImage,
      Transform transform,
      ExportAreaPictureAnnotation3DPan panToHighlight) {
    var panImage = BufferedImageFactory.make(baseImage);
    var g2d = Graphics2DFactory.make(panImage);

    drawPan(g2d, transform, SELECTED_PAN_COLOR, panToHighlight);

    g2d.dispose();

    return panImage;
  }

  private static void drawPan(
      Graphics2D g2d, Transform transform, Color fillColor, ExportAreaPictureAnnotation3DPan pan) {
    var polygon = Coordinates.from(requireNonNull(pan.getPolygon()));
    polygon = transform.apply(polygon);

    drawFillPolygon(g2d, fillColor, polygon);
    drawStrokePolygon(g2d, transform, pan, 1f);
  }

  public BufferedImage generateBaseImageWithHighlightedPan(
      BufferedImage baseImage,
      Transform transform,
      ExportAreaPictureAnnotation3DPan panToHighlight) {
    var panImage = BufferedImageFactory.make(baseImage);
    var g2d = Graphics2DFactory.make(panImage);

    var polygon = Coordinates.from(requireNonNull(panToHighlight.getPolygon()));
    polygon = transform.apply(polygon);

    drawFillPolygon(g2d, SELECTED_PAN_COLOR, polygon);
    drawStrokePolygon(g2d, BLACK, POLYGON_STROKE, polygon);

    g2d.dispose();

    return panImage;
  }

  public BufferedImage generatePanImageWithMeasurements(ExportAreaPictureAnnotation3DPan pan) {
    var panImageWithMeasurements = BufferedImageFactory.make(TARGET_SIZE, TARGET_SIZE);
    var g2d = Graphics2DFactory.make(panImageWithMeasurements);

    var coordinates = Coordinates.from(pan.getPolygon());
    var transform = Transform.from(coordinates, CONTENT_SIZE - 10, TARGET_SIZE);
    var mapped = transform.apply(coordinates);

    drawStrokePolygon(g2d, transform, pan, 3.5f);
    drawPolygonPoints(g2d, BLACK, POLYGON_POINTS_SIZE, mapped);
    drawPolygonMeasurements(
        g2d,
        MEASUREMENT_CONF,
        mapped,
        pan.getMeasurements(),
        panImageWithMeasurements.getWidth(),
        panImageWithMeasurements.getHeight());

    g2d.dispose();
    return panImageWithMeasurements;
  }

  public BufferedImage mergePanImagesSideBySide(
      BufferedImage panImage, BufferedImage panImageWithMeasurements) {
    int width = panImage.getWidth() + panImageWithMeasurements.getWidth();
    int height = Math.max(panImage.getHeight(), panImageWithMeasurements.getHeight());
    var mergedImage = BufferedImageFactory.make(width, height);

    var g2d = Graphics2DFactory.make(mergedImage);

    g2d.drawImage(panImage, 0, 0, null);
    g2d.drawImage(panImageWithMeasurements, panImage.getWidth(), 0, null);

    g2d.dispose();
    return mergedImage;
  }
}
