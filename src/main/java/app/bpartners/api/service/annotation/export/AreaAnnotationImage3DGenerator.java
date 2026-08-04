package app.bpartners.api.service.annotation.export;

import static app.bpartners.api.service.annotation.factory.RoofSlopeBoundaryFactory.selectPolygon;
import static app.bpartners.api.service.annotation.model.Drawer.*;
import static java.awt.Color.*;
import static java.awt.Font.PLAIN;
import static java.util.Objects.requireNonNull;

import app.bpartners.api.model.annotation.IntXY;
import app.bpartners.api.service.annotation.AreaAnnotation3DPan;
import app.bpartners.api.service.annotation.AreaAnnotationInstanceInfo;
import app.bpartners.api.service.annotation.factory.AnnotationSummaryFactory;
import app.bpartners.api.service.annotation.factory.BufferedImageFactory;
import app.bpartners.api.service.annotation.factory.Graphics2DFactory;
import app.bpartners.api.service.annotation.model.Coordinates;
import app.bpartners.api.service.annotation.model.Pair;
import app.bpartners.api.service.annotation.model.Transform;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class AreaAnnotationImage3DGenerator {
  private static final int TARGET_SIZE = 550;
  private static final int SUMMARY_IMAGE_SIZE = 1080;
  // Content must be smaller than the target image so the drawn stroke (up to 3px, centered on
  // the path) is not clipped at the image edge: when contentSize == targetSize the padding is 0
  // and width-bound roofs map their max-X vertex to x == image width (out of bounds).
  private static final int SUMMARY_IMAGE_MARGIN = 20;
  private static final int SUMMARY_CONTENT_SIZE = SUMMARY_IMAGE_SIZE - 2 * SUMMARY_IMAGE_MARGIN;
  private static final int CONTENT_SIZE = 500;

  private static final int POLYGON_POINTS_SIZE = 10;
  private static final Stroke POLYGON_STROKE = new BasicStroke(3f);
  private static final Color SELECTED_PAN_COLOR = new Color(229, 142, 25);
  private static final String FONT_NAME = "Arial"; // TODO: sync with pdf font
  private static final Font SUMMARY_IMAGE_FONT = new Font(FONT_NAME, PLAIN, 15);
  private static final MeasurementConf MEASUREMENT_CONF =
      MeasurementConf.builder()
          .offset(new IntXY(0, 6))
          .textColor(BLACK)
          .bgColor(new Color(200, 200, 200, 150))
          .font(new Font(FONT_NAME, PLAIN, 35))
          .build();

  public Pair<Transform, BufferedImage> generateBaseImage(List<AreaAnnotation3DPan> pans) {
    var imageContext = getImageContext(pans);

    pans.forEach(
        pan -> {
          var rawData = Coordinates.from(requireNonNull(pan.getPolygon()));
          var mapped = imageContext.transform.apply(rawData);

          drawFillPolygon(imageContext.g2d, RED, mapped);
          drawStrokePolygon(imageContext.g2d, WHITE, POLYGON_STROKE, mapped);
        });

    imageContext.g2d.dispose();

    return new Pair<>(imageContext.transform, imageContext.baseImage);
  }

  public Pair<Transform, BufferedImage> generateBaseImageWithSlopeBoundariesWithMeasurement(
      List<AreaAnnotation3DPan> pans) {
    var imageContext = getImageContext(pans);

    pans.forEach(
        pan -> {
          drawStrokePolygon(imageContext.g2d, imageContext.transform, pan, 1f, false);
          var coordinates = Coordinates.from(pan.getPolygon());
          var mapped = imageContext.transform.apply(coordinates);
          drawPolygonMeasurements(
              imageContext.g2d,
              MEASUREMENT_CONF.toBuilder().bgColor(null).font(SUMMARY_IMAGE_FONT).build(),
              mapped,
              pan.getMeasurements(),
              imageContext.baseImage.getWidth(),
              imageContext.baseImage.getHeight(),
              false);
        });

    imageContext.g2d.dispose();

    return new Pair<>(imageContext.transform, imageContext.baseImage);
  }

  public BufferedImage generateBaseImageWithAreas(List<AreaAnnotation3DPan> pans) {
    var imageContext = getImageContext(pans);

    pans.forEach(
        pan -> {
          var area = "-";
          var optionalArea =
              pan.getInfos().stream()
                  .filter(m -> m.label().toLowerCase().startsWith("surface rampant"))
                  .findFirst();
          if (optionalArea.isPresent()) {
            area = optionalArea.get().value().replace("m²", "").replace("m", "");
          }

          drawPanWithCenteredText(imageContext, pan, area);
        });

    imageContext.g2d.dispose();

    return imageContext.baseImage;
  }

  public BufferedImage generateBaseImageWithPitches(List<AreaAnnotation3DPan> pans) {
    var imageContext = getImageContext(pans);

    pans.forEach(
        pan -> {
          var pitch = "-";
          var optionalPitch =
              pan.getInfos().stream()
                  .filter(m -> m.label().toLowerCase().startsWith("pente"))
                  .map(AreaAnnotationInstanceInfo::value)
                  .map(AnnotationSummaryFactory::formatPitch)
                  .findFirst();
          if (optionalPitch.isPresent()) {
            pitch = optionalPitch.get();
          }

          drawPanWithCenteredText(imageContext, pan, pitch);
        });

    imageContext.g2d.dispose();

    return imageContext.baseImage;
  }

  private void drawPanWithCenteredText(
      ImageContext imageContext, AreaAnnotation3DPan pan, String pitch) {
    var coordinates = Coordinates.from(pan.getPolygon());
    var mapped = imageContext.transform.apply(coordinates);
    drawStrokePolygon(imageContext.g2d, BLACK, POLYGON_STROKE, mapped);
    drawTextInPolygonCentroid(
        imageContext.g2d,
        MEASUREMENT_CONF.toBuilder().bgColor(null).font(SUMMARY_IMAGE_FONT).build(),
        mapped,
        pitch,
        imageContext.baseImage.getWidth(),
        imageContext.baseImage.getHeight());
  }

  public BufferedImage generateBaseImageWithNames(List<AreaAnnotation3DPan> pans) {
    ImageContext imageContext = getImageContext(pans);

    for (int index = 0; index < pans.size(); index++) {
      var pan = pans.get(index);
      var name = "P" + (index + 1);

      var coordinates = Coordinates.from(pan.getPolygon());
      var mapped = imageContext.transform().apply(coordinates);
      var font = new Font(FONT_NAME, PLAIN, SUMMARY_IMAGE_FONT.getSize() * 2);
      drawStrokePolygon(imageContext.g2d(), BLACK, POLYGON_STROKE, mapped);
      drawTextInPolygonCentroid(
          imageContext.g2d(),
          MEASUREMENT_CONF.toBuilder().bgColor(null).font(font).build(),
          mapped,
          name,
          imageContext.baseImage().getWidth(),
          imageContext.baseImage().getHeight());
    }

    imageContext.g2d().dispose();

    return imageContext.baseImage();
  }

  public BufferedImage generateBaseImageWithHighlightedPanWithSlopeBoundary(
      BufferedImage baseImage, Transform transform, AreaAnnotation3DPan panToHighlight) {
    var panImage = BufferedImageFactory.make(baseImage);
    var g2d = Graphics2DFactory.make(panImage);

    drawPan(g2d, transform, SELECTED_PAN_COLOR, panToHighlight);

    g2d.dispose();

    return panImage;
  }

  private static void drawPan(
      Graphics2D g2d, Transform transform, Color fillColor, AreaAnnotation3DPan pan) {
    var polygon = Coordinates.from(requireNonNull(pan.getPolygon()));
    polygon = transform.apply(polygon);

    drawFillPolygon(g2d, fillColor, polygon);
    drawStrokePolygon(g2d, transform, pan, 1f, false);
  }

  public BufferedImage generateBaseImageWithHighlightedPan(
      BufferedImage baseImage, Transform transform, AreaAnnotation3DPan panToHighlight) {
    var panImage = BufferedImageFactory.make(baseImage);
    var g2d = Graphics2DFactory.make(panImage);

    var polygon = Coordinates.from(requireNonNull(panToHighlight.getPolygon()));
    polygon = transform.apply(polygon);

    drawFillPolygon(g2d, SELECTED_PAN_COLOR, polygon);
    drawStrokePolygon(g2d, BLACK, POLYGON_STROKE, polygon);

    g2d.dispose();

    return panImage;
  }

  public BufferedImage generatePanImageWithMeasurements(AreaAnnotation3DPan pan) {
    var panImageWithMeasurements = BufferedImageFactory.make(TARGET_SIZE, TARGET_SIZE);
    var g2d = Graphics2DFactory.make(panImageWithMeasurements);

    var polygon = selectPolygon(pan, true);
    var coordinates = Coordinates.from(polygon);
    var transform = Transform.from(coordinates, CONTENT_SIZE - 10, TARGET_SIZE);
    var mapped = transform.apply(coordinates);

    drawStrokePolygon(g2d, transform, pan, 3.5f, true);
    drawPolygonPoints(g2d, BLACK, POLYGON_POINTS_SIZE, mapped);
    drawPolygonMeasurements(
        g2d,
        MEASUREMENT_CONF,
        mapped,
        pan.getMeasurements(),
        panImageWithMeasurements.getWidth(),
        panImageWithMeasurements.getHeight(),
        true);

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

  private static @NotNull ImageContext getImageContext(List<AreaAnnotation3DPan> pans) {
    var allPoints = extractPoints(pans);

    var allX = allPoints.stream().mapToInt(IntXY::x).toArray();
    var allY = allPoints.stream().mapToInt(IntXY::y).toArray();
    var transform =
        Transform.from(new Coordinates(allX, allY), CONTENT_SIZE * 2, SUMMARY_IMAGE_SIZE);

    var baseImage = BufferedImageFactory.make(SUMMARY_IMAGE_SIZE, SUMMARY_IMAGE_SIZE);
    var g2d = Graphics2DFactory.make(baseImage);
    ImageContext imageContext = new ImageContext(transform, baseImage, g2d);
    return imageContext;
  }

  private record ImageContext(Transform transform, BufferedImage baseImage, Graphics2D g2d) {}

  private static @NotNull List<IntXY> extractPoints(List<AreaAnnotation3DPan> pans) {
    return pans.stream()
        .flatMap(
            pan ->
                requireNonNull(pan.getPolygon()).points().stream()
                    .map(p -> new IntXY((int) Math.round(p.x()), (int) Math.round(p.y()))))
        .toList();
  }
}
