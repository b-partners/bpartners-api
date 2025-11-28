package app.bpartners.api.service.annotation.factory;

import java.awt.image.BufferedImage;

public class BufferedImageFactory {
  private BufferedImageFactory() {}

  public static BufferedImage make(BufferedImage original) {
    var copy = new BufferedImage(original.getWidth(), original.getHeight(), original.getType());

    var g2 = Graphics2DFactory.make(copy);
    g2.drawImage(original, 0, 0, null);
    g2.dispose();

    return copy;
  }
}
