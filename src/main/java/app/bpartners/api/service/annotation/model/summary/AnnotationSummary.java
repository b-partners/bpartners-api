package app.bpartners.api.service.annotation.model.summary;

import java.util.List;

public record AnnotationSummary(
    String diagramImageBase64,
    String baseImageWithAreasUri,
    String baseImageWithNamesUri,
    String baseImageWithPitchesUri,
    List<AnnotationMeasurementSummary> measurements,
    List<AnnotationPitch> pitchBreakdown,
    List<AnnotationWaste> wasteTable,
    List<AnnotationRoofSlopeSummary> faces,
    String suggestedWastePercent) {}
