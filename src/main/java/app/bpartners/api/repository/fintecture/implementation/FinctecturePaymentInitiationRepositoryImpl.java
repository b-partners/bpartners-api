package app.bpartners.api.repository.fintecture.implementation;

import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.fintecture.model.PaymentInitiation;
import app.bpartners.api.repository.fintecture.model.PaymentRedirection;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.endpoint.rest.security.swan.SwanConf.BEARER_PREFIX;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.util.UUID.randomUUID;

@Repository
@AllArgsConstructor
public class FinctecturePaymentInitiationRepositoryImpl implements
    FintecturePaymentInitiationRepository {
  public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
  private final FintectureConf fintectureConf;
  private final ProjectTokenManager tokenManager;

  @Override
  public PaymentRedirection save(PaymentInitiation paymentReq, String redirectUri) {
    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      String urlParams = String.format("?redirectUri=%s", redirectUri);
      String data = new ObjectMapper().writeValueAsString(paymentReq);
      String requestId = String.valueOf(randomUUID());
      String digest = getDigest(data);
      String date = getParsedDate();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(fintectureConf.getRequestToPayUrl() + urlParams))
          .header("Content-Type", "application/json")
          .header("Accept", "application/json")
          .header("x-request-id", requestId)
          .header("x-language", "fr")
          .header("digest", digest)
          .header("Date", date)
          .header("Signature", getHeaderSignature(requestId, digest, date, urlParams))
          .header("Authorization", BEARER_PREFIX + tokenManager.getFintectureProjectToken())
          .POST(HttpRequest.BodyPublishers.ofString(data))
          .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return new ObjectMapper()
          .findAndRegisterModules() //Load DateTime Module
          .readValue(response.body(), PaymentRedirection.class);
    } catch (IOException | URISyntaxException | NoSuchAlgorithmException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private String getParsedDate() {
    String rfc2822Pattern = "EEE, dd MMM yyyy HH:mm:ss Z";
    SimpleDateFormat format = new SimpleDateFormat(rfc2822Pattern);
    return format.format(Date.from(Instant.now()));
  }

  private String getDigest(String payload) throws NoSuchAlgorithmException {
    MessageDigest msgDigest = MessageDigest.getInstance("SHA-256");
    byte[] hash = msgDigest.digest(payload.getBytes(StandardCharsets.UTF_8));
    String hashString = Base64.getEncoder().encodeToString(hash);
    return "SHA-256=" + hashString;
  }

  private String getHeaderSignature(
      String requestId, String digest, String date, String urlParams) {
    return "keyId=\"" + fintectureConf.getAppId() + "\","
        + "algorithm=\"rsa-sha256\",headers=\"(request-target) date digest x-request-id\","
        + "signature=\"" + getSignature(requestId, digest, date, urlParams) + "\"";
  }

  private String getSignature(String requestId, String digest, String date, String urlParams) {
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

  private static PrivateKey copyKey(String privateKey)
      throws NoSuchAlgorithmException, InvalidKeySpecException {
    byte[] keyAsBytes = Base64.getDecoder().decode(privateKey);
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyAsBytes);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    return kf.generatePrivate(spec);
  }
}
