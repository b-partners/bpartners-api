package app.bpartners.api.service.annotation.factory;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Graphics2DFactory {
  private Graphics2DFactory() {}

  public static Graphics2D make(BufferedImage image) {
    var g2d = image.createGraphics();

    g2d.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    return g2d;
  }
}
