package app.bpartners.api.service.WMS.imageSource;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.WMS.imageSource.exception.BlankImageException;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class ImageValidator implements Consumer<File> {
  public static final int WHITE_COLOR_RGB = Color.WHITE.getRGB();

  @Override
  public void accept(File file) throws BlankImageException {
    try {
      Image image = ImageIO.read(file);
      image = image.getScaledInstance(100, -1, Image.SCALE_FAST);
      int w = image.getWidth(null);
      int h = image.getHeight(null);
      int[] pixels = new int[w * h];
      PixelGrabber pg = new PixelGrabber(image, 0, 0, w, h, pixels, 0, w);
      pg.grabPixels();
      for (int pixel : pixels) {
        Color color = new Color(pixel);
        if (color.getAlpha() == 0 || color.getRGB() != WHITE_COLOR_RGB) {
          return;
        }
      }
    } catch (IOException | InterruptedException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    throw new BlankImageException("Image from " + file.getName() + " is blank");
  }
}
