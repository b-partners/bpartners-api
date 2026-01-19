package app.bpartners.api.service.user.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

public record AnalysisApiKeyCreation(
    String consumerName,
    String consumerEmail,
    ConsumerType consumerType,
    Double maxSurface,
    List<DetectableObjectModel> allowedModels,
    @JsonIgnore List<AuthorizedZone> authorizedZones,
    @JsonIgnore List<DetectableObjectType> detectableObjectTypes) {}
