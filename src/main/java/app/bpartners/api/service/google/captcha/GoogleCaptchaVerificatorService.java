package app.bpartners.api.service.google.captcha;

import app.bpartners.api.model.GoogleCaptchaResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Slf4j
public class GoogleCaptchaVerificatorService {
  private final String baseUrl;
  private final RestTemplate restTemplate;
  private final String recaptchaSecret;

  public GoogleCaptchaVerificatorService(
      RestTemplate restTemplate,
      @Value("${google.captcha.secret}") String recaptchaSecret,
      @Value("${google.captcha.url}") String recaptchaBaseUrl) {
    this.restTemplate = restTemplate;
    this.recaptchaSecret = recaptchaSecret;
    this.baseUrl = recaptchaBaseUrl;
  }

  public boolean verifyToken(String token) {
    String url =
        UriComponentsBuilder.fromHttpUrl(baseUrl)
            .queryParam("secret", recaptchaSecret)
            .queryParam("response", token)
            .toUriString();

    ResponseEntity<GoogleCaptchaResponse> response =
        restTemplate.postForEntity(url, null, GoogleCaptchaResponse.class);

    GoogleCaptchaResponse data = response.getBody();
    log.info("GoogleCaptchaResponse={}", data);

    return data != null && data.isSuccess() && data.getScore() != null && data.getScore() > 0.7;
  }
}
