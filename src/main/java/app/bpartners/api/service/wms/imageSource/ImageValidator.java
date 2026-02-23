package app.bpartners.api.service.wms.imageSource;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.wms.imageSource.exception.BlankImageException;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ImageValidator implements Consumer<File> {
  public void accept(File file) throws BlankImageException {
    log.info("File content: {}", file);

    if (file == null) {
      throw new ApiException(SERVER_EXCEPTION, "File is null");
    }

    try {
      BufferedImage image = ImageIO.read(file);
      if (image == null) {
        throw new ApiException(SERVER_EXCEPTION, "Image is null");
      }

      int w = image.getWidth();
      int h = image.getHeight();
      int[] pixels = image.getRGB(0, 0, w, h, null, 0, w);

      boolean isBlank = true;
      boolean isBlack = true;
      boolean hasRed = false;

      for (int pixel : pixels) {

        if ((pixel & 0xFF000000) != 0 && !isWhite(pixel)) {
          isBlank = false;
        }

        if (pixel != Color.BLACK.getRGB()) {
          isBlack = false;
        }

        if (isRed(pixel)) {
          hasRed = true;
        }

        if (!isBlank && !isBlack && hasRed) {
          break;
        }
      }

      if (isBlank) {
        throw new BlankImageException("Image from " + file.getName() + " is blank");
      }

      if (isBlack) {
        throw new BlankImageException("Image from " + file.getName() + " is completely black");
      }

      if (hasRed) {
        throw new BlankImageException("Image from " + file.getName() + " contains red pixels");
      }

    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private boolean isRed(int pixel) {
    int r = (pixel >> 16) & 0xFF;
    int g = (pixel >> 8) & 0xFF;
    int b = pixel & 0xFF;

    return r > 150 && g < 100 && b < 100;
  }

  private boolean isWhite(int pixel) {
    return (pixel & 0xFFFFFF) == 0xFFFFFF;
  }
}
