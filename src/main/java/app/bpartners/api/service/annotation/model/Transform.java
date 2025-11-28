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
  private final IntXY offset;
  private final double scale;

  public Coordinates apply(Coordinates coordinates) {
    var allX = coordinates.allX();
    var allY = coordinates.allY();

    var resultAllX = new int[allX.length];
    var resultAllY = new int[allY.length];

    for (int i = 0; i < allX.length; i++) {
      var x = allX[i];
      var y = allY[i];

      int nx = (int) Math.round((x - min.x()) * scale + offset.x());
      int ny = (int) Math.round((y - min.y()) * scale + offset.y());

      resultAllX[i] = nx;
      resultAllY[i] = ny;
    }

    return new Coordinates(resultAllX, resultAllY);
  }

  public static Transform from(Coordinates polygon, int contentSize, int targetSize) {
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
        .offset(new IntXY(paddingX, paddingY))
        .build();
  }
}
