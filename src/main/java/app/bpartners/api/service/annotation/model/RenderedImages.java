package app.bpartners.api.service.annotation.model;

import java.util.List;

/** Base64 images ready for the template: the main image and one sub image per section. */
public record RenderedImages(String main, List<String> subs) {}
