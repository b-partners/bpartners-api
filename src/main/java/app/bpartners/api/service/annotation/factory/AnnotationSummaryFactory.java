package app.bpartners.api.service.annotation.factory;

import static app.bpartners.api.service.annotation.utils.ImageUriUtils.bufferedImageToUri;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstanceInfo;
import app.bpartners.api.endpoint.rest.model.Point;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.model.summary.AnnotationMeasurementSummary;
import app.bpartners.api.service.annotation.model.summary.AnnotationPitch;
import app.bpartners.api.service.annotation.model.summary.AnnotationRoofSlopeSummary;
import app.bpartners.api.service.annotation.model.summary.AnnotationSummary;
import app.bpartners.api.service.annotation.model.summary.AnnotationWaste;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnnotationSummaryFactory {
  private AnnotationSummaryFactory() {}

  private static final String UNKNOWN_VALUE_PLACEHOLDER = "-";
  private static final double[] WASTE_PERCENTS = {
    0d, 2.5d, 5d, 7.5d, 10d, 12.5d, 15d, 17.5d, 20d, 22.5d
  };
  // Matches the +10% column already used in the per-face summary below.
  private static final double SUGGESTED_WASTE_PERCENT = 10d;

  public static AnnotationSummary create(
      ExportAreaPictureAnnotation annotation,
      ExportAreaPictureAnnotationImage3DGenerator generator) {
    if (annotation.get3d() == null) {
      return null;
    }

    var diagramImage =
        generator
            .generateBaseImageWithSlopeBoundariesWithMeasurement(annotation.get3d().getPans())
            .second();
    var baseImageWithAreas = generator.generateBaseImageWithAreas(annotation.get3d().getPans());
    var baseImageWithNames = generator.generateBaseImageWithNames(annotation.get3d().getPans());
    var baseImageWithPitches = generator.generateBaseImageWithPitches(annotation.get3d().getPans());

    String baseImageWithRoofSlopeBoundariesUri = bufferedImageToUri(diagramImage);
    String baseImageWithAreasUri = bufferedImageToUri(baseImageWithAreas);
    String baseImageWithNamesUri = bufferedImageToUri(baseImageWithNames);
    String baseImageWithPitchesUri = bufferedImageToUri(baseImageWithPitches);
    List<AnnotationRoofSlopeSummary> faces = faces(annotation);
    List<AnnotationMeasurementSummary> measurements = getMeasurementsSummary(faces, annotation);
    List<AnnotationPitch> pitchBreakdown = pitchBreakdown(faces);
    List<AnnotationWaste> wasteTable = wasteTable(faces);

    return new AnnotationSummary(
        baseImageWithRoofSlopeBoundariesUri,
        baseImageWithAreasUri,
        baseImageWithNamesUri,
        baseImageWithPitchesUri,
        measurements,
        pitchBreakdown,
        wasteTable,
        faces,
        formatPercent(SUGGESTED_WASTE_PERCENT));
  }

  private static List<AnnotationMeasurementSummary> getMeasurementsSummary(
      List<AnnotationRoofSlopeSummary> roofSlopes, ExportAreaPictureAnnotation annotation) {
    var measurementSummaries = new ArrayList<AnnotationMeasurementSummary>();

    // Sum all pan areas — faces() stores clean "%.2f" numeric strings, no unit suffix
    double totalArea = totalArea(roofSlopes);
    String totalAreaFormatted =
        totalArea > 0 ? String.format("%.2fm²", totalArea) : UNKNOWN_VALUE_PLACEHOLDER;

    measurementSummaries.add(
        new AnnotationMeasurementSummary(
            "Surface (rampant) totale de la toiture", totalAreaFormatted));

    measurementSummaries.add(
        new AnnotationMeasurementSummary("Nombre de pans", String.valueOf(roofSlopes.size())));

    String dominantPitch =
        roofSlopes.stream()
            .filter(
                face ->
                    !UNKNOWN_VALUE_PLACEHOLDER.equals(face.area())
                        && !UNKNOWN_VALUE_PLACEHOLDER.equals(face.pitch()))
            .max(
                Comparator.comparingDouble(
                    face -> {
                      try {
                        return Double.parseDouble(face.area());
                      } catch (Exception e) {
                        return 0d;
                      }
                    }))
            .map(face -> face.pitch() + "°")
            .orElse(UNKNOWN_VALUE_PLACEHOLDER);

    measurementSummaries.add(new AnnotationMeasurementSummary("Pente dominante", dominantPitch));
    measurementSummaries.addAll(slopeBoundariesSummary(annotation));

    return measurementSummaries;
  }

  private static List<AnnotationMeasurementSummary> slopeBoundariesSummary(
      ExportAreaPictureAnnotation annotation) {
    var edgeTypesCount = new HashMap<String, Integer>();
    var edgeTypesSize = new HashMap<String, Double>();
    // Adjacent pans each carry their own copy of a shared edge (e.g. a ridge between two roof
    // faces), so the same physical edge must only be counted once across all pans.
    var seenEdgeKeys = new HashSet<String>();

    annotation
        .get3d()
        .getPans()
        .forEach(
            pan -> {
              var typeNames = RoofSlopeBoundaryFactory.getRoofSlopeBoundaryTypeNames(pan);
              var panMeasurements = pan.getMeasurements();
              var points = pan.getPolygon().getPoints();
              for (int index = 0; index < typeNames.size(); index++) {
                var name = typeNames.get(index);
                var measurements = panMeasurements.get(index);
                var edgeKey = name + ":" + edgeKey(points.get(index), points.get(index + 1));
                if (!seenEdgeKeys.add(edgeKey)) {
                  continue;
                }
                var count = edgeTypesCount.getOrDefault(name, 0);
                var size = edgeTypesSize.getOrDefault(name, 0d);
                edgeTypesCount.put(name, count + 1);
                edgeTypesSize.put(name, size + measurements.getValue());
              }
            });

    return edgeTypesCount.keySet().stream()
        .map(
            key ->
                new AnnotationMeasurementSummary(
                    key.substring(0, 1).toUpperCase() + key.substring(1).replace("-", " "),
                    String.format(
                        Locale.ROOT,
                        "%.2f m (%s)",
                        edgeTypesSize.get(key),
                        edgeTypesCount.get(key))))
        .toList();
  }

  // Points are shared across all pans of the same annotation (see
  // ExportAreaPictureAnnotationImage3DGenerator, which pools every pan's polygon points into a
  // single bounding box), so two edges with the same rounded endpoints, in either order, are the
  // same physical edge.
  private static String edgeKey(Point a, Point b) {
    var pa = roundedPoint(a);
    var pb = roundedPoint(b);
    return pa.compareTo(pb) <= 0 ? pa + "|" + pb : pb + "|" + pa;
  }

  private static String roundedPoint(Point point) {
    return String.format("%.3f,%.3f", point.getX(), point.getY());
  }

  private static List<AnnotationWaste> wasteTable(List<AnnotationRoofSlopeSummary> faces) {
    double totalArea = totalArea(faces);
    if (totalArea == 0) {
      return List.of();
    }

    return Arrays.stream(WASTE_PERCENTS)
        .mapToObj(
            percent ->
                new AnnotationWaste(
                    percent == SUGGESTED_WASTE_PERCENT,
                    formatPercent(percent),
                    String.format(Locale.ROOT, "%.2f", totalArea * (1 + percent / 100.0))))
        .toList();
  }

  private static double totalArea(List<AnnotationRoofSlopeSummary> faces) {
    return faces.stream().mapToDouble(AnnotationSummaryFactory::parseArea).sum();
  }

  private static double parseArea(AnnotationRoofSlopeSummary face) {
    try {
      return Double.parseDouble(face.area());
    } catch (Exception e) {
      return 0d;
    }
  }

  private static String formatPercent(double percent) {
    return String.format(Locale.ROOT, "%.1f %%", percent);
  }

  private static List<AnnotationPitch> pitchBreakdown(List<AnnotationRoofSlopeSummary> faces) {
    record FaceData(String pitch, double area) {}

    List<FaceData> parseable =
        faces.stream()
            .filter(
                f ->
                    !UNKNOWN_VALUE_PLACEHOLDER.equals(f.pitch())
                        && !UNKNOWN_VALUE_PLACEHOLDER.equals(f.area()))
            .map(
                f -> {
                  try {
                    double area = Double.parseDouble(f.area());
                    return new FaceData(f.pitch(), area);
                  } catch (Exception e) {
                    log.warn("Could not parse area '{}' for pitch breakdown", f.area());
                    return null;
                  }
                })
            .filter(fd -> fd != null)
            .toList();

    double totalArea = parseable.stream().mapToDouble(FaceData::area).sum();
    if (totalArea == 0) {
      return List.of();
    }

    Map<String, Double> areaByPitch = new LinkedHashMap<>();
    for (FaceData fd : parseable) {
      areaByPitch.merge(fd.pitch(), fd.area(), Double::sum);
    }

    return areaByPitch.entrySet().stream()
        .sorted(Map.Entry.<String, Double>comparingByValue().reversed()) // largest group first
        .map(
            entry -> {
              double groupArea = entry.getValue();
              double pct = (groupArea / totalArea) * 100.0;
              return new AnnotationPitch(
                  entry.getKey() + "°",
                  String.format("%.2f m²", groupArea),
                  String.format("%.1f %%", pct));
            })
        .toList();
  }

  private static List<AnnotationRoofSlopeSummary> faces(ExportAreaPictureAnnotation annotation) {
    List<ExportAreaPictureAnnotation3DPan> pans = annotation.get3d().getPans();

    record RawFace(String name, String pitchFormatted, double areaParsed) {}

    List<RawFace> raw = new ArrayList<>();
    for (int index = 0; index < pans.size(); index++) {
      var pan = pans.get(index);

      String pitchFormatted =
          pan.getInfos().stream()
              .filter(info -> info.getLabel().toLowerCase().startsWith("pente"))
              .findFirst()
              .map(ExportAreaPictureAnnotationInstanceInfo::getValue)
              .map(AnnotationSummaryFactory::formatPitch)
              .orElse(UNKNOWN_VALUE_PLACEHOLDER);

      int finalIndex = index;
      double areaParsed =
          pan.getInfos().stream()
              .filter(info -> info.getLabel().toLowerCase().startsWith("surface rampant"))
              .findFirst()
              .map(ExportAreaPictureAnnotationInstanceInfo::getValue)
              .map(
                  v -> {
                    try {
                      return Double.parseDouble(strip(v).replace("m²", "").replace("m", ""));
                    } catch (Exception e) {
                      log.warn("Could not parse area '{}' for face P{}", v, (finalIndex + 1));
                      return 0d;
                    }
                  })
              .orElse(0d);

      raw.add(new RawFace("P" + (index + 1), pitchFormatted, areaParsed));
    }

    double totalArea = raw.stream().mapToDouble(RawFace::areaParsed).sum();

    List<AnnotationRoofSlopeSummary> faces = new ArrayList<>();
    for (RawFace rf : raw) {
      boolean hasArea = rf.areaParsed() > 0;

      String areaFormatted =
          hasArea ? String.format("%.2f", rf.areaParsed()) : UNKNOWN_VALUE_PLACEHOLDER;
      String roofPercent =
          (hasArea && totalArea > 0)
              ? String.format("%.1f%%", (rf.areaParsed() / totalArea) * 100.0)
              : UNKNOWN_VALUE_PLACEHOLDER;
      String area10 =
          hasArea ? String.format("%.2f", rf.areaParsed() * 1.10) : UNKNOWN_VALUE_PLACEHOLDER;
      String area20 =
          hasArea ? String.format("%.2f", rf.areaParsed() * 1.20) : UNKNOWN_VALUE_PLACEHOLDER;

      faces.add(
          AnnotationRoofSlopeSummary.builder()
              .name(rf.name())
              .pitch(rf.pitchFormatted())
              .area(areaFormatted)
              .roofPercent(roofPercent)
              .area10(area10)
              .area20(area20)
              .build());
    }
    return faces;
  }

  public static String formatMeasure(String rawMeasure) {
    try {
      return String.format("%.2f", Double.parseDouble(strip(rawMeasure).replace("m", "")));
    } catch (Exception e) {
      log.error("Error while formatting measure {}", rawMeasure, e);
      return rawMeasure;
    }
  }

  public static String formatArea(String rawArea) {
    try {
      return String.format(
          "%.2f", Double.parseDouble(strip(rawArea).replace("m²", "").replace("m", "")));
    } catch (Exception e) {
      log.error("Error while formatting area {}", rawArea, e);
      return rawArea;
    }
  }

  public static String formatPitch(String rawPitch) {
    try {
      var pitch = Double.parseDouble(strip(rawPitch).replace("°", ""));
      return String.format("%d", (int) pitch);
    } catch (Exception e) {
      log.error("Error while formatting pitch {}", rawPitch, e);
      return rawPitch;
    }
  }

  private static String strip(String str) {
    return str.replace(" ", "");
  }
}
