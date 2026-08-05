package app.bpartners.api.service.annotation;

import static app.bpartners.api.service.annotation.factory.RoofSlopeBoundaryFactory.selectPolygon;
import static app.bpartners.api.service.annotation.model.Drawer.*;
import static java.awt.Color.*;
import static java.awt.Font.PLAIN;
import static java.util.Objects.requireNonNull;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstanceInfo;
import app.bpartners.api.model.annotation.IntXY;
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
public class ExportAreaPictureAnnotationImage3DGenerator {
  private static final int TARGET_SIZE = 550;
  private static final int SUMMARY_IMAGE_SIZE = 1080;
  private static final int CONTENT_SIZE = 500;
  private static final int PAN_CONTENT_SIZE = CONTENT_SIZE - 10;

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

  public Pair<Transform, BufferedImage> generateBaseImage(
      List<ExportAreaPictureAnnotation3DPan> pans) {
    var imageContext = getImageContext(pans);

    pans.forEach(
        pan -> {
          var rawData = Coordinates.from(requireNonNull(pan.getPolygon()));
          var mapped = imageContext.transform().apply(rawData);

          drawFillPolygon(imageContext.g2d(), RED, mapped);
          drawStrokePolygon(imageContext.g2d(), WHITE, POLYGON_STROKE, mapped);
        });

    imageContext.g2d().dispose();

    return new Pair<>(imageContext.transform(), imageContext.baseImage());
  }

  public Pair<Transform, BufferedImage> generateBaseImageWithSlopeBoundariesWithMeasurement(
      List<ExportAreaPictureAnnotation3DPan> pans) {
    ImageContext imageContext = getImageContext(pans);

    pans.forEach(
        pan -> {
          drawStrokePolygon(imageContext.g2d(), imageContext.transform(), pan, 1f, false);
          var coordinates = Coordinates.from(pan.getPolygon());
          var mapped = imageContext.transform().apply(coordinates);
          drawPolygonMeasurements(
              imageContext.g2d(),
              MEASUREMENT_CONF.toBuilder().bgColor(null).font(SUMMARY_IMAGE_FONT).build(),
              mapped,
              pan.getMeasurements(),
              imageContext.baseImage().getWidth(),
              imageContext.baseImage().getHeight(),
              false);
        });

    imageContext.g2d().dispose();

    return new Pair<>(imageContext.transform(), imageContext.baseImage());
  }

  private static @NotNull ImageContext getImageContext(
      List<ExportAreaPictureAnnotation3DPan> pans) {
    var allPoints = extractPoints(pans);

    var allX = allPoints.stream().mapToInt(IntXY::x).toArray();
    var allY = allPoints.stream().mapToInt(IntXY::y).toArray();
    var transform =
        Transform.from(new Coordinates(allX, allY), CONTENT_SIZE * 2, SUMMARY_IMAGE_SIZE);

    var baseImage = BufferedImageFactory.make(SUMMARY_IMAGE_SIZE, SUMMARY_IMAGE_SIZE);
    var g2d = Graphics2DFactory.make(baseImage);
    return new ImageContext(transform, baseImage, g2d);
  }

  private static @NotNull ImageContext getImageContext(ExportAreaPictureAnnotation3DPan pan) {
    var coordinates = pixelCoordinatesToCartesian(pan);
    var transform = Transform.from(coordinates, PAN_CONTENT_SIZE, TARGET_SIZE);

    var baseImage = BufferedImageFactory.make(TARGET_SIZE, TARGET_SIZE);
    var g2d = Graphics2DFactory.make(baseImage);
    return new ImageContext(transform, baseImage, g2d);
  }

  private record ImageContext(Transform transform, BufferedImage baseImage, Graphics2D g2d) {}

  public BufferedImage generateBaseImageWithAreas(List<ExportAreaPictureAnnotation3DPan> pans) {
    var imageContext = getImageContext(pans);

    pans.forEach(
        pan -> {
          var area = "-";
          var optionalArea =
              pan.getInfos().stream()
                  .filter(m -> m.getLabel().toLowerCase().startsWith("surface rampant"))
                  .findFirst();
          if (optionalArea.isPresent()) {
            area = optionalArea.get().getValue().replace("m²", "").replace("m", "");
          }

          drawPolygonWithCenteredText(imageContext, pan, area);
        });

    imageContext.g2d().dispose();

    return imageContext.baseImage();
  }

  private void drawPolygonWithCenteredText(
      ImageContext imageContext, ExportAreaPictureAnnotation3DPan pan, String area) {
    var coordinates = Coordinates.from(pan.getPolygon());
    var mapped = imageContext.transform().apply(coordinates);
    drawStrokePolygon(imageContext.g2d(), BLACK, POLYGON_STROKE, mapped);
    drawTextInPolygonCentroid(
        imageContext.g2d(),
        MEASUREMENT_CONF.toBuilder().bgColor(null).font(SUMMARY_IMAGE_FONT).build(),
        mapped,
        area,
        imageContext.baseImage().getWidth(),
        imageContext.baseImage().getHeight());
  }

  public BufferedImage generateBaseImageWithPitches(List<ExportAreaPictureAnnotation3DPan> pans) {
    var imageContext = getImageContext(pans);

    pans.forEach(
        pan -> {
          var pitch = "-";
          var optionalPitch =
              pan.getInfos().stream()
                  .filter(m -> m.getLabel().toLowerCase().startsWith("pente"))
                  .map(ExportAreaPictureAnnotationInstanceInfo::getValue)
                  .map(AnnotationSummaryFactory::formatPitch)
                  .findFirst();
          if (optionalPitch.isPresent()) {
            pitch = optionalPitch.get();
          }

          drawPolygonWithCenteredText(imageContext, pan, pitch);
        });

    imageContext.g2d().dispose();

    return imageContext.baseImage();
  }

  public BufferedImage generateBaseImageWithNames(List<ExportAreaPictureAnnotation3DPan> pans) {
    var imageContext = getImageContext(pans);

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

  private static @NotNull List<IntXY> extractPoints(List<ExportAreaPictureAnnotation3DPan> pans) {
    var rawPoints =
        pans.stream()
            .flatMap(pan -> requireNonNull(pan.getPolygon().getPoints()).stream())
            .toList();

    int maxY =
        rawPoints.stream().mapToInt(p -> requireNonNull(p.getY()).intValue()).max().orElse(0);

    return rawPoints.stream()
        .map(
            p ->
                new IntXY(
                    requireNonNull(p.getX()).intValue(),
                    maxY - requireNonNull(p.getY()).intValue()))
        .toList();
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
    drawStrokePolygon(g2d, transform, pan, 1f, false);
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
    var imageContext = getImageContext(pan);
    Coordinates coordinates = pixelCoordinatesToCartesian(pan);
    var mapped = imageContext.transform().apply(coordinates);

    drawStrokePolygon(imageContext.g2d(), imageContext.transform(), pan, 3.5f, true);
    drawPolygonPoints(imageContext.g2d(), BLACK, POLYGON_POINTS_SIZE, mapped);
    drawPolygonMeasurements(
        imageContext.g2d(),
        MEASUREMENT_CONF,
        mapped,
        pan.getMeasurements(),
        imageContext.baseImage().getWidth(),
        imageContext.baseImage().getHeight(),
        true);

    imageContext.g2d().dispose();
    return imageContext.baseImage();
  }

  private static Coordinates pixelCoordinatesToCartesian(ExportAreaPictureAnnotation3DPan pan) {
    var polygon = selectPolygon(pan, true);
    var rawCoordinates = Coordinates.from(polygon);

    int maxY = 0;
    for (int y : rawCoordinates.allY()) {
      if (y > maxY) {
        maxY = y;
      }
    }

    int[] invertedY = new int[rawCoordinates.allY().length];
    for (int i = 0; i < rawCoordinates.allY().length; i++) {
      invertedY[i] = maxY - rawCoordinates.allY()[i];
    }

    return new Coordinates(rawCoordinates.allX(), invertedY);
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
