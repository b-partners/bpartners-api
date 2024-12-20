package app.bpartners.api.service.WMS;

import app.bpartners.api.endpoint.rest.model.ZoomLevel;
import java.util.stream.IntStream;
import lombok.Getter;

/**
 * Summary: ArcGisVectorTileLayer enum was created using <a
 * href="https://wiki.openstreetmap.org/wiki/Zoom_levels">OSM ZoomLevels</a> <br>
 * and <a
 * href="https://developers.arcgis.com/documentation/mapping-apis-and-services/reference/zoom-levels-and-scale">ArcGis
 * docs</a>.
 */
@Getter
public enum ArcgisZoom {
  WORLD_0(0),
  WORLD_1(1),
  WORLD_2(2),
  CONTINENT_0(3),
  CONTINENT_1(4),
  COUNTRIES(5),
  COUNTRY(6),
  STATES(7),
  COUNTIES_0(8),
  COUNTIES_1(9),
  COUNTY(10),
  METROPOLITAN_AREA(11),
  CITIES(12),
  CITY(13),
  TOWN(14),
  NEIGHBORHOOD(15),
  STREETS(16),
  CITY_BLOCK(17),
  BUILDINGS(18),
  BUILDING(19),
  HOUSES_0(20),
  HOUSES_1(21),
  HOUSES_2(22),
  HOUSE_PROPERTY(23);
  final int zoomLevel;

  ArcgisZoom(int zoomLevel) {
    this.zoomLevel = zoomLevel;
  }

  ArcgisZoom(ZoomLevel zoomLevel) {
    this.zoomLevel = zoomLevel.ordinal();
  }

  public static ArcgisZoom from(int zoomLevel) {
    return switch (zoomLevel) {
      case 0 -> WORLD_0;
      case 1 -> WORLD_1;
      case 2 -> WORLD_2;
      case 3 -> CONTINENT_0;
      case 4 -> CONTINENT_1;
      case 5 -> COUNTRIES;
      case 6 -> COUNTRY;
      case 7 -> STATES;
      case 8 -> COUNTIES_0;
      case 9 -> COUNTIES_1;
      case 10 -> COUNTY;
      case 11 -> METROPOLITAN_AREA;
      case 12 -> CITIES;
      case 13 -> CITY;
      case 14 -> TOWN;
      case 15 -> NEIGHBORHOOD;
      case 16 -> STREETS;
      case 17 -> CITY_BLOCK;
      case 18 -> BUILDINGS;
      case 19 -> BUILDING;
      case 20 -> HOUSES_0;
      case 21 -> HOUSES_1;
      case 22 -> HOUSES_2;
      case 23 -> HOUSE_PROPERTY;
      default ->
          throw new IllegalArgumentException("Zoom Level = " + zoomLevel + " not supported.");
    };
  }

  public static ArcgisZoom from(ZoomLevel zoomLevel) {
    return switch (zoomLevel) {
      case WORLD_0 -> WORLD_0;
      case WORLD_1 -> WORLD_1;
      case WORLD_2 -> WORLD_2;
      case CONTINENT_0 -> CONTINENT_0;
      case CONTINENT_1 -> CONTINENT_1;
      case COUNTRIES -> COUNTRIES;
      case COUNTRY -> COUNTRY;
      case STATES -> STATES;
      case COUNTIES_0 -> COUNTIES_0;
      case COUNTIES_1 -> COUNTIES_1;
      case COUNTY -> COUNTY;
      case METROPOLITAN_AREA -> METROPOLITAN_AREA;
      case CITIES -> CITIES;
      case CITY -> CITY;
      case TOWN -> TOWN;
      case NEIGHBORHOOD -> NEIGHBORHOOD;
      case STREETS -> STREETS;
      case CITY_BLOCK -> CITY_BLOCK;
      case BUILDINGS -> BUILDINGS;
      case BUILDING -> BUILDING;
      case HOUSES_0 -> HOUSES_0;
      case HOUSES_1 -> HOUSES_1;
      case HOUSES_2 -> HOUSES_2;
      case HOUSE_PROPERTY -> HOUSE_PROPERTY;
      default ->
          throw new IllegalArgumentException("Zoom Level = " + zoomLevel + " not supported.");
    };
  }

  public static ArcgisZoom[] zoomLevels() {
    return IntStream.rangeClosed(0, 23)
        .boxed()
        .map(ArcgisZoom::from)
        .sorted()
        .toArray(ArcgisZoom[]::new);
  }
}
