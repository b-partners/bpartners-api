package app.bpartners.api.service.annotation.model.summary;

import java.util.List;

public record AnnotationSummary(
    String diagramImageBase64,
    List<AnnotationMeasurementSummary> measurements,
    List<AnnotationPitch> pitchBreakdown,
    List<AnnotationWaste> wasteTable,
    List<AnnotationRoofFaceSummary> faces,
    String suggestedWastePercent) {}
