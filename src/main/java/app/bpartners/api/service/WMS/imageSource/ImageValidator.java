package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.WMS.imageSource.exception.BlankImageException;
import java.awt.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class ImageValidator implements Consumer<File> {
  public void accept(File file) throws BlankImageException {
    try {
      BufferedImage image = ImageIO.read(file);
      int w = image.getWidth();
      int h = image.getHeight();
      int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);
      boolean isBlank = true;
      boolean isBlack = true;

      for (int pixel : pixels) {
        if ((pixel & 0xFF000000) != 0 && !isWhite(pixel)) {
          isBlank = false;
        }
        if (pixel != Color.BLACK.getRGB()) {
          isBlack = false;
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

  private boolean isWhite(int pixel) {
    return (pixel & 0xFFFFFF) == 0xFFFFFF;
  }
}
