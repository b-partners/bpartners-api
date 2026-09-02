package app.bpartners.api.service.annotation.model;

import java.util.Arrays;

public record Coordinates(int[] allX, int[] allY) {
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Coordinates other)) {
      return false;
    }
    return Arrays.equals(allX, other.allX) && Arrays.equals(allY, other.allY);
  }

  @Override
  public int hashCode() {
    return 31 * Arrays.hashCode(allX) + Arrays.hashCode(allY);
  }

  @Override
  public String toString() {
    return "Coordinates[allX=" + Arrays.toString(allX) + ", allY=" + Arrays.toString(allY) + "]";
  }
}
