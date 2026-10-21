package app.bpartners.api.service.annotation;

import app.bpartners.api.model.exception.BadRequestException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import org.springframework.stereotype.Component;

@Component
public class ImageDownloader {
  // Source photos can be far larger than the ~1180px target ImageCompressor eventually resizes
  // to; decoding them at full resolution just to immediately throw that resolution away wastes
  // memory. Cap the decode size, but with enough headroom that quality isn't visibly affected.
  private static final int MAX_DECODE_DIMENSION = 2500;

  public record DownloadedImage(BufferedImage image, int trueWidth, int trueHeight) {}

  public DownloadedImage downloadImage(String imageUrl) {
    try (var inputStream = new URI(imageUrl).toURL().openStream();
        var imageInputStream = ImageIO.createImageInputStream(inputStream)) {
      var readers = ImageIO.getImageReaders(imageInputStream);
      if (!readers.hasNext()) {
        throw new BadRequestException("Cannot read the image from the url");
      }
      ImageReader reader = readers.next();
      try {
        reader.setInput(imageInputStream, true, true);
        int trueWidth = reader.getWidth(0);
        int trueHeight = reader.getHeight(0);
        var readParam = reader.getDefaultReadParam();
        int subsampling = Math.max(1, Math.max(trueWidth, trueHeight) / MAX_DECODE_DIMENSION);
        if (subsampling > 1) {
          readParam.setSourceSubsampling(subsampling, subsampling, 0, 0);
        }
        return new DownloadedImage(reader.read(0, readParam), trueWidth, trueHeight);
      } finally {
        reader.dispose();
      }
    } catch (IOException | URISyntaxException e) {
      throw new BadRequestException("Cannot read the image from the url");
    }
  }

  public byte[] downloadImageBytes(String imageUrl) {
    if (imageUrl == null) {
      return null;
    }
    try (var inputStream = new URI(imageUrl).toURL().openStream()) {
      return inputStream.readAllBytes();
    } catch (IOException | URISyntaxException e) {
      throw new BadRequestException("Cannot read the image from the url");
    }
  }
}
