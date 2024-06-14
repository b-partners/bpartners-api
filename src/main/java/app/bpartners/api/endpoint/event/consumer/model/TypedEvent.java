package app.bpartners.api.endpoint.event.consumer.model;

import app.bpartners.api.PojaGenerated;
import app.bpartners.api.endpoint.event.model.PojaEvent;

@PojaGenerated
public record TypedEvent(String typeName, PojaEvent payload) {}
