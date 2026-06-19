package app.bpartners.api.service.annotation.factory;

import static app.bpartners.api.service.annotation.utils.ImageUriUtils.bufferedImageToUri;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotation3DPan;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstanceInfo;
import app.bpartners.api.service.annotation.ExportAreaPictureAnnotationImage3DGenerator;
import app.bpartners.api.service.annotation.model.summary.AnnotationMeasurementSummary;
import app.bpartners.api.service.annotation.model.summary.AnnotationPitch;
import app.bpartners.api.service.annotation.model.summary.AnnotationRoofSlopeSummary;
import app.bpartners.api.service.annotation.model.summary.AnnotationSummary;
import app.bpartners.api.service.annotation.model.summary.AnnotationWaste;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnnotationSummaryFactory {
  private static final String UNKNOWN_VALUE_PLACEHOLDER = "-";

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

    String baseImageWithRoofSlopeBoundariesUri = bufferedImageToUri(diagramImage);
    String baseImageWithAreasUri = bufferedImageToUri(baseImageWithAreas);
    String baseImageWithNamesUri = bufferedImageToUri(baseImageWithNames);
    List<AnnotationRoofSlopeSummary> faces = faces(annotation);
    List<AnnotationMeasurementSummary> measurements = getMeasurementsSummary(faces, annotation);
    List<AnnotationPitch> pitchBreakdown = pitchBreakdown(faces);
    List<AnnotationWaste> wasteTable = wasteTable(annotation);
    String suggestedWastePercent = suggestedWastePercent(annotation);

    return new AnnotationSummary(
        baseImageWithRoofSlopeBoundariesUri,
        baseImageWithAreasUri,
        baseImageWithNamesUri,
        measurements,
        pitchBreakdown,
        wasteTable,
        faces,
        suggestedWastePercent);
  }

  private static List<AnnotationMeasurementSummary> getMeasurementsSummary(
      List<AnnotationRoofSlopeSummary> roofSlopes, ExportAreaPictureAnnotation annotation) {
    var measurementSummaries = new ArrayList<AnnotationMeasurementSummary>();

    // Sum all pan areas — faces() stores clean "%.2f" numeric strings, no unit suffix
    double totalArea =
        roofSlopes.stream()
            .mapToDouble(
                face -> {
                  try {
                    return Double.parseDouble(face.area());
                  } catch (Exception e) {
                    return 0d;
                  }
                })
            .sum();
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

    annotation
        .get3d()
        .getPans()
        .forEach(
            pan -> {
              var typeNames = RoofSlopeBoundaryFactory.getRoofSlopeBoundaryTypeNames(pan);
              var panMeasurements = pan.getMeasurements();
              for (int index = 0; index < typeNames.size(); index++) {
                var name = typeNames.get(index);
                var measurements = panMeasurements.get(index);
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
                    String.format("%.2f m² (%s)", edgeTypesSize.get(key), edgeTypesCount.get(key))))
        .toList();
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

  private static List<AnnotationWaste> wasteTable(
      ExportAreaPictureAnnotation annotation) { // TODO: instruction unclear
    return null;
  }

  private static String suggestedWastePercent(
      ExportAreaPictureAnnotation annotation) { // TODO: instruction unclear
    return null;
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
