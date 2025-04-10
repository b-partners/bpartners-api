package app.bpartners.api.model.detection;

import app.bpartners.api.model.User;
import java.time.Instant;

public record DetectionTracking(
    String id,
    String zone,
    String address,
    Instant creationDatetime,
    DetectionInitiator detectionInitiator,
    User user) {}
