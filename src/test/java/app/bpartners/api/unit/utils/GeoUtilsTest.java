package app.bpartners.api.unit.utils;

import app.bpartners.api.service.utils.GeoUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeoUtilsTest {
  private static GeoUtils.Coordinate coordinate1() {
    return new GeoUtils.Coordinate(48.867503703133984, 2.270368312109328);
  }

  private static GeoUtils.Coordinate coordinate2() {
    return new GeoUtils.Coordinate(48.86919457328631, 2.2716855924110484);
  }

  @Test
  void compute_distance_km_ok() {
    assertEquals(0.21126616566096512,
        GeoUtils.computeDistanceKm(
            coordinate1(),
            coordinate2()));
  }

  @Test
  void compute_distance_m_ok() {
    assertEquals(211.3, GeoUtils.computeDistanceM(coordinate1(), coordinate2()));
  }

  @Test
  void compute_distance_from_coordinates() {
    assertEquals(211.3, coordinate1().getDistanceFrom(coordinate2()));
    assertEquals(0, coordinate1().getDistanceFrom(coordinate1()));
  }
}
