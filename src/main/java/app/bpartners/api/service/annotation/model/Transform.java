package app.bpartners.api.service.annotation.model;

import app.bpartners.api.model.annotation.IntXY;
import java.util.Arrays;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder(toBuilder = true)
@RequiredArgsConstructor
public class Transform {
  private final double minX;
  private final double minY;
  private final double maxX;
  private final double maxY;
  private final IntXY offset;
  private final double scale;

  @Builder.Default private final boolean flipY = true;
  @Builder.Default private final boolean flipX = false;

  public Coordinates apply(RawCoordinates coordinates) {
    var allX = coordinates.allX();
    var allY = coordinates.allY();

    var resultAllX = new int[allX.length];
    var resultAllY = new int[allY.length];

    for (int i = 0; i < allX.length; i++) {
      var x = allX[i];
      var y = allY[i];

      int nx =
          flipX
              ? (int) Math.round((maxX - x) * scale + offset.x())
              : (int) Math.round((x - minX) * scale + offset.x());

      int ny =
          flipY
              ? (int) Math.round((maxY - y) * scale + offset.y())
              : (int) Math.round((y - minY) * scale + offset.y());

      resultAllX[i] = nx;
      resultAllY[i] = ny;
    }

    return new Coordinates(resultAllX, resultAllY);
  }

  public static Transform from(RawCoordinates polygon, int contentSize, int targetSize) {
    return yFlippedFrom(polygon, contentSize, targetSize, true);
  }

  public static Transform yFlippedFrom(
      RawCoordinates polygon, int contentSize, int targetSize, boolean flipY) {
    return from(polygon, contentSize, targetSize, flipY, false);
  }

  public static Transform from(
      RawCoordinates polygon, int contentSize, int targetSize, boolean flipY, boolean flipX) {
    var allX = polygon.allX();
    var allY = polygon.allY();

    double minX = Arrays.stream(allX).min().orElse(0);
    double minY = Arrays.stream(allY).min().orElse(0);
    double maxX = Arrays.stream(allX).max().orElse(0);
    double maxY = Arrays.stream(allY).max().orElse(0);

    double width = Math.max(1e-6, maxX - minX);
    double height = Math.max(1e-6, maxY - minY);

    double scale = Math.min(contentSize / width, contentSize / height);

    int scaledWidth = (int) Math.round(width * scale);
    int scaledHeight = (int) Math.round(height * scale);

    int paddingX = (targetSize - scaledWidth) / 2;
    int paddingY = (targetSize - scaledHeight) / 2;

    return Transform.builder()
        .scale(scale)
        .minX(minX)
        .minY(minY)
        .maxX(maxX)
        .maxY(maxY)
        .offset(new IntXY(paddingX, paddingY))
        .flipY(flipY)
        .flipX(flipX)
        .build();
  }
}
