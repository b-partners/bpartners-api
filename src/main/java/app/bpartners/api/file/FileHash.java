package app.bpartners.api.file;

import app.bpartners.api.PojaGenerated;

@PojaGenerated
public record FileHash(FileHashAlgorithm algorithm, String value) {}
