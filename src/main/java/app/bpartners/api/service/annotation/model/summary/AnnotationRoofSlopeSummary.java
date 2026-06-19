package app.bpartners.api.service.annotation.model.summary;

import lombok.Builder;

@Builder
public record AnnotationRoofSlopeSummary(
    String name, String area, String pitch, String roofPercent, String area10, String area20) {}
