package app.bpartners.api.file;

import app.bpartners.api.PojaGenerated;

@PojaGenerated
@SuppressWarnings("all")
public record FileHash(FileHashAlgorithm algorithm, String value) {}
