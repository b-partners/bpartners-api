package app.bpartners.api.service.geojobs;

import app.bpartners.api.endpoint.rest.model.GeoJobsCoverageType;
import java.util.List;

public record GeoJobsCoverageQuery(
    String address, Double longitude, Double latitude, List<GeoJobsCoverageType> types) {}
