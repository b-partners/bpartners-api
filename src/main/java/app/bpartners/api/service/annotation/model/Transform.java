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
  private final IntXY min;
  private final IntXY max;
  private final IntXY offset;
  private final double scale;

  @Builder.Default private final boolean flipY = true;
  @Builder.Default private final boolean flipX = false;

  public Coordinates apply(Coordinates coordinates) {
    var allX = coordinates.allX();
    var allY = coordinates.allY();

    var resultAllX = new int[allX.length];
    var resultAllY = new int[allY.length];

    for (int i = 0; i < allX.length; i++) {
      var x = allX[i];
      var y = allY[i];

      int nx =
          flipX
              ? (int) Math.round((max.x() - x) * scale + offset.x())
              : (int) Math.round((x - min.x()) * scale + offset.x());

      int ny =
          flipY
              ? (int) Math.round((max.y() - y) * scale + offset.y())
              : (int) Math.round((y - min.y()) * scale + offset.y());

      resultAllX[i] = nx;
      resultAllY[i] = ny;
    }

    return new Coordinates(resultAllX, resultAllY);
  }

  public static Transform from(Coordinates polygon, int contentSize, int targetSize) {
    return yFlippedFrom(polygon, contentSize, targetSize, true);
  }

  public static Transform yFlippedFrom(
      Coordinates polygon, int contentSize, int targetSize, boolean flipY) {
    return from(polygon, contentSize, targetSize, flipY, false);
  }

  public static Transform from(
      Coordinates polygon, int contentSize, int targetSize, boolean flipY, boolean flipX) {
    var allX = polygon.allX();
    var allY = polygon.allY();

    int minX = Arrays.stream(allX).min().orElse(0);
    int minY = Arrays.stream(allY).min().orElse(0);
    int maxX = Arrays.stream(allX).max().orElse(0);
    int maxY = Arrays.stream(allY).max().orElse(0);

    int width = Math.max(1, maxX - minX);
    int height = Math.max(1, maxY - minY);

    double scale = Math.min((double) contentSize / width, (double) contentSize / height);

    int scaledWidth = (int) Math.round(width * scale);
    int scaledHeight = (int) Math.round(height * scale);

    int paddingX = (targetSize - scaledWidth) / 2;
    int paddingY = (targetSize - scaledHeight) / 2;

    return Transform.builder()
        .scale(scale)
        .min(new IntXY(minX, minY))
        .max(new IntXY(maxX, maxY))
        .offset(new IntXY(paddingX, paddingY))
        .flipY(flipY)
        .flipX(flipX)
        .build();
  }
}
