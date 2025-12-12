package app.bpartners.api.service.annotation.factory;

import app.bpartners.api.model.exception.BadRequestException;
import java.awt.*;

public class ColorFactory {
  private ColorFactory() {}

  private static final int HEXADECIMAL_COLOR_LENGTH_WITH_OPACITY = 9;
  private static final int HEXADECIMAL_COLOR_LENGTH_WITHOUT_OPACITY = 7;

  public static Color make(String hexColor) {
    if (hexColor == null) {
      throw new BadRequestException("Wrong color format was received");
    }

    if (!hexColor.startsWith("#")
        || (hexColor.length() != HEXADECIMAL_COLOR_LENGTH_WITHOUT_OPACITY
            && hexColor.length() != HEXADECIMAL_COLOR_LENGTH_WITH_OPACITY)) {
      throw new BadRequestException("Wrong color format was received");
    }

    hexColor = hexColor.substring(1);
    int red = Integer.valueOf(hexColor.substring(0, 2), 16);
    int green = Integer.valueOf(hexColor.substring(2, 4), 16);
    int blue = Integer.valueOf(hexColor.substring(4, 6), 16);
    int alpha = hexColor.length() == 8 ? Integer.valueOf(hexColor.substring(6, 8), 16) : 255;
    return new Color(red, green, blue, alpha);
  }
}
