package app.bpartners.api.service.geoserver;

public record GeoserverWmsGetMapQuery(
    String service,
    String version,
    String request,
    String layers,
    String styles,
    String format,
    Boolean transparent,
    String srs,
    String crs,
    String bbox,
    Integer width,
    Integer height,
    String token,
    Long expires) {}
