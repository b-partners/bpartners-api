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
  private static final int TARGET_SIZE = 600;
  private static final int CONTENT_SIZE = 500;

  private static final int POLYGON_POINTS_SIZE = 10;
  private static final Stroke POLYGON_STROKE = new BasicStroke(3f);
  private static final MeasurementConf MEASUREMENT_CONF =
      MeasurementConf.builder()
          .offset(new IntXY(0, 6))
          .textColor(WHITE)
          .bgColor(new Color(0, 0, 0, 150))
          .font(new Font("Arial", PLAIN, 22))
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

    var baseImage = new BufferedImage(TARGET_SIZE, TARGET_SIZE, BufferedImage.TYPE_INT_ARGB);
    var g2d = Graphics2DFactory.make(baseImage);

    pans.forEach(
        pan -> {
          var rawData = Coordinates.from(requireNonNull(pan.getPolygon()));
          var mapped = transform.apply(rawData);

          drawFillPolygon(g2d, RED, mapped);
        });

    g2d.dispose();

    return new Pair<>(transform, baseImage);
  }

  public BufferedImage generatePanImage(
      BufferedImage baseImage, Transform transform, ExportAreaPictureAnnotation3DPan pan) {
    var panImage = BufferedImageFactory.make(baseImage);
    var g2d = Graphics2DFactory.make(panImage);

    var polygon = Coordinates.from(requireNonNull(pan.getPolygon()));
    polygon = transform.apply(polygon);

    drawFillPolygon(g2d, ORANGE, polygon);
    drawStrokePolygon(g2d, BLACK, POLYGON_STROKE, polygon);
    drawPolygonPoints(g2d, BLACK, POLYGON_POINTS_SIZE, polygon);
    drawPolygonMeasurements(g2d, MEASUREMENT_CONF, polygon, pan.getMeasurements());

    g2d.dispose();
    return panImage;
  }
}
