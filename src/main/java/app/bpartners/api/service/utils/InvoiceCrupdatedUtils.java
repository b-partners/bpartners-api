package app.bpartners.api.service.utils;

import app.bpartners.api.model.exception.ApiException;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import javax.imageio.ImageIO;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.utils.FileInfoUtils.JPG_FORMAT_NAME;
import static com.google.zxing.BarcodeFormat.QR_CODE;

@NoArgsConstructor
@Slf4j
public class InvoiceCrupdatedUtils {
  public static byte[] generateQrCode(String textOrLink) {
    int width = 1000; //TODO-skip-for-now: make this size parameterizable
    int height = 1000; //TODO-skip-for-now: make this size parameterizable
    try {
      QRCodeWriter writer = new QRCodeWriter();
      BitMatrix bitMatrix = writer.encode(textOrLink, QR_CODE, width, height);
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      ImageIO.write(MatrixToImageWriter.toBufferedImage(bitMatrix), JPG_FORMAT_NAME, os);
      return os.toByteArray();
    } catch (WriterException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public static String base64Image(byte[] image) {
    return Base64.getEncoder().encodeToString(image);
  }
}