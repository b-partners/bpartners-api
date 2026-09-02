package app.bpartners.api.service.annotation.model;

/** Polygon coordinates already mapped to pixel space by {@link Transform#apply(RawCoordinates)}. */
public record Coordinates(int[] allX, int[] allY) {}
