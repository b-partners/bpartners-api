package app.bpartners.api.repository.fintecture.implementation.utils;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.fintecture.FintectureConf;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

public class FintecturePaymentUtils {
  public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
  public static final String REQUEST_ID = "x-request-id";
  public static final String LANGUAGE = "x-language";
  public static final String SIGNATURE = "Signature";
  public static final String AUTHORIZATION = "Authorization";
  public static final String DATE = "Date";
  public static final String ACCEPT = "accept";
  public static final String APPLICATION_JSON = "application/json";

  private FintecturePaymentUtils() {
  }

  public static String getParsedDate() {
    String rfc2822Pattern = "EEE, dd MMM yyyy HH:mm:ss Z";
    SimpleDateFormat format = new SimpleDateFormat(rfc2822Pattern);
    return format.format(Date.from(Instant.now()));
  }

  public static String getDigest(String payload) throws NoSuchAlgorithmException {
    MessageDigest msgDigest = MessageDigest.getInstance("SHA-256");
    byte[] hash = msgDigest.digest(payload.getBytes(StandardCharsets.UTF_8));
    String hashString = Base64.getEncoder().encodeToString(hash);
    return "SHA-256=" + hashString;
  }

  public static String getHeaderSignatureWithDigest(FintectureConf fintectureConf, String requestId,
                                                    String digest, String date, String urlParams) {
    return "keyId=\"" + fintectureConf.getAppId() + "\","
        + "algorithm=\"rsa-sha256\",headers=\"(request-target)"
        + " date digest x-request-id\", signature=\""
        + getSignatureWithDigest(fintectureConf, requestId, digest, date, urlParams) + "\"";
  }

  public static String getHeaderSignature(FintectureConf fintectureConf, String requestId,
                                          String date, String urlParams) {
    return "keyId=\"" + fintectureConf.getAppId() + "\","
        + "algorithm=\"rsa-sha256\",headers=\"(request-target) date x-request-id\","
        + "signature=\"" + getSignature(fintectureConf, requestId, date, urlParams) + "\"";
  }

  public static String getSignatureWithDigest(FintectureConf fintectureConf, String requestId,
                                              String digest,
                                              String date, String urlParams) {
    try {
      String signingString =
          "(request-target): post /pis/v2/request-to-pay" + urlParams + "\n"
              + "date: " + date + "\n"
              + "digest: " + digest + "\n"
              + "x-request-id: " + requestId;
      PrivateKey key = copyKey(fintectureConf.getPrivateKey());
      Signature privateSignature = Signature.getInstance(SIGNATURE_ALGORITHM);
      privateSignature.initSign(key);
      privateSignature.update(signingString.getBytes(StandardCharsets.UTF_8));
      byte[] signatureAsBytes = privateSignature.sign();
      return Base64.getEncoder().encodeToString(signatureAsBytes);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException | SignatureException
             | InvalidKeyException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public static String getSignature(FintectureConf fintectureConf, String requestId,
                                    String date, String urlParams) {
    try {
      String signingString =
          "(request-target): get /pis/v2/payments" + urlParams + "\n"
              + "date: " + date + "\n"
              + "x-request-id: " + requestId;
      PrivateKey key = copyKey(fintectureConf.getPrivateKey());
      Signature privateSignature = Signature.getInstance(SIGNATURE_ALGORITHM);
      privateSignature.initSign(key);
      privateSignature.update(signingString.getBytes(StandardCharsets.UTF_8));
      byte[] signatureAsBytes = privateSignature.sign();
      return Base64.getEncoder().encodeToString(signatureAsBytes);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException | SignatureException
             | InvalidKeyException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private static PrivateKey copyKey(String privateKey)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] keyAsBytes = Base64.getDecoder().decode(privateKey);
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyAsBytes);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    return kf.generatePrivate(spec);
  }
}
