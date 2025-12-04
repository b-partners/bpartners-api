package app.bpartners.api.unit.google;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.GoogleCaptchaResponse;
import app.bpartners.api.service.google.captcha.GoogleCaptchaVerificatorService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class GoogleCaptchaVerificatorServiceTest {
  RestTemplate restTemplate = mock(RestTemplate.class);
  String secretToken = "dummy";
  String baseUrlMock = "https://www.google.com/";
  GoogleCaptchaVerificatorService subject =
      new GoogleCaptchaVerificatorService(restTemplate, secretToken, baseUrlMock);

  @Test
  void verifyCaptcha() {
    GoogleCaptchaResponse mockResponse =
        GoogleCaptchaResponse.builder().success(true).score(0.8).build();

    ResponseEntity<GoogleCaptchaResponse> responseEntity =
        new ResponseEntity<>(mockResponse, HttpStatus.OK);

    when(restTemplate.postForEntity(anyString(), any(), eq(GoogleCaptchaResponse.class)))
        .thenReturn(responseEntity);

    boolean isVerified = subject.verifyToken("dummy");

    assertTrue(isVerified);
  }
}
