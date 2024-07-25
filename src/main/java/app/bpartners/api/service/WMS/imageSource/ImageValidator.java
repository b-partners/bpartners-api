package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.WMS.imageSource.exception.BlankImageException;
import java.awt.*;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class ImageValidator implements Consumer<File> {
  public static final int WHITE_COLOR_RGB = Color.WHITE.getRGB();

  public void accept(File file) throws BlankImageException {
    try {
      BufferedImage image = ImageIO.read(file);
      int w = image.getWidth();
      int h = image.getHeight();
      boolean isBlank = true;
      boolean isBlack = true;

      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          int pixel = image.getRGB(x, y);
          Color color = new Color(pixel, true); // Get color with alpha

          if (color.getAlpha() > 0 && !isWhite(color)) {
            isBlank = false;
          }
          if (color.getRGB() != Color.BLACK.getRGB()) {
            isBlack = false;
          }
          if (!isBlank && !isBlack) {
            break;
          }
        }
        if (!isBlank && !isBlack) {
          break;
        }
      }

      if (isBlank) {
        throw new BlankImageException("Image from " + file.getName() + " is blank");
      }
      if (isBlack) {
        throw new BlankImageException("Image from " + file.getName() + " is completely black");
      }
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private boolean isWhite(Color color) {
    return color.getRed() == 255 && color.getGreen() == 255 && color.getBlue() == 255;
  }

  private BufferedImage scaleImage(BufferedImage image, int width, int height) {
    Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_FAST);
    BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    Graphics2D graphics = bufferedImage.createGraphics();
    graphics.drawImage(scaledImage, 0, 0, null);
    graphics.dispose();

    return bufferedImage;
  }
}
