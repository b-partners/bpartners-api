package app.bpartners.api.service.annotation.export;

public record CompressionParameters(
    long originalSize, int targetWidth, int targetHeight, float quality) {}
