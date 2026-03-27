package app.bpartners.api.service.annotation;

public record CompressionParameters(
    long originalSize, int targetWidth, int targetHeight, float quality) {}
