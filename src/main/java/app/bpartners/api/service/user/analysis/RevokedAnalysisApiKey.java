package app.bpartners.api.service.user.analysis;

import java.time.Instant;

public record RevokedAnalysisApiKey(String keyValue, Instant revokedAt) {}
