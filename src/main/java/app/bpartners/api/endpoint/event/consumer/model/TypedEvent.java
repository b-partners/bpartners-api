package app.bpartners.api.endpoint.event.consumer.model;

import app.bpartners.api.PojaGenerated;

@PojaGenerated
public record TypedEvent(String typeName, Object payload) {}
