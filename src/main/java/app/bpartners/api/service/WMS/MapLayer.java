package app.bpartners.api.service.WMS;

import lombok.Getter;

@Getter
public enum MapLayer {
  TOUS_FR("tous_fr", "ALL", 0);

  private final String value;
  private final String zoneName;
  private final int year;

  MapLayer(String value, String zoneName, int year) {
    this.zoneName = zoneName;
    this.value = value;
    this.year = year;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  public static MapLayer fromValue(String value) {
    for (MapLayer b : MapLayer.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }

  public static MapLayer from(app.bpartners.api.endpoint.rest.model.OpenStreetMapLayer rest) {
    return fromValue(rest.getValue());
  }
}
