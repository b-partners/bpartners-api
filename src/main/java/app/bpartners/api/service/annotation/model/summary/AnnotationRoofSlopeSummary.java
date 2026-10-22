package app.bpartners.api.service.annotation.model.summary;

import lombok.Builder;

@Builder
public record AnnotationRoofSlopeSummary(
    String name,
    String area,
    String pitch,
    String roofPercent,
    String areaPlus10Percent,
    String areaPlus20Percent) {}
