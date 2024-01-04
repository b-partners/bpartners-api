package app.bpartners.api.service.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

public class GeoUtils {
  private GeoUtils() {}

  public static double computeDistanceKm(Coordinate coordinate1, Coordinate coordinate2) {

    // The math module contains a function
    // named toRadians which converts from
    // degrees to radians.
    double lon1 = Math.toRadians(coordinate1.getLongitude());
    double lon2 = Math.toRadians(coordinate2.getLongitude());
    double lat1 = Math.toRadians(coordinate1.getLatitude());
    double lat2 = Math.toRadians(coordinate2.getLatitude());

    // Haversine formula
    double dlon = lon2 - lon1;
    double dlat = lat2 - lat1;
    double a =
        Math.pow(Math.sin(dlat / 2), 2)
            + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon / 2), 2);

    double c = 2 * Math.asin(Math.sqrt(a));

    // Radius of earth in kilometers. Use 3956
    // for miles
    double r = 6371;

    // calculate the result
    return (c * r);
  }

  public static double computeDistanceM(Coordinate coordinate1, Coordinate coordinate2) {
    BigDecimal roundedDecimal =
        BigDecimal.valueOf(computeDistanceKm(coordinate1, coordinate2) * 1000.0)
            .setScale(1, RoundingMode.HALF_UP);
    return roundedDecimal.doubleValue();
  }

  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @ToString
  @EqualsAndHashCode
  public static class Coordinate {
    private Double latitude;
    private Double longitude;

    public Double getDistanceFrom(Coordinate other) {
      return other == null || other.getLatitude() == null || other.getLongitude() == null
          ? null
          : computeDistanceM(this, other);
    }
  }
}
