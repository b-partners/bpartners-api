package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.service.google.captcha.GoogleCaptchaVerificatorService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class CaptchaVerificatorController {
  private GoogleCaptchaVerificatorService googleCaptchaVerificatorService;

  @GetMapping("/captcha/token")
  public boolean verifyCaptcha(@RequestParam(name = "token") String token) {
    return googleCaptchaVerificatorService.verifyToken(token);
  }
}
