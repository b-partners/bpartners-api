package app.bpartners.api.endpoint.rest.controller;

import static app.bpartners.api.service.utils.SecurityUtils.API_KEY_HEADER;

import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.subscription.BillingGranularity;
import app.bpartners.api.service.subscription.SubscriptionBillingStatsHtmlRenderer;
import app.bpartners.api.service.subscription.SubscriptionBillingStatsService;
import app.bpartners.api.service.subscription.SubscriptionBillingTimeSeriesService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class SubscriptionBillingStatsController {
  private static final ZoneId PARIS_ZONE = ZoneId.of("Europe/Paris");
  private static final String DASHBOARD_PATH = "/subscriptionBillingStats";
  private static final String SIGNIN_PATH = "/subscriptionBillingStats/signin";
  private static final String COOKIE_PATH = "/subscriptionBillingStats";

  private final SubscriptionBillingStatsService subscriptionBillingStatsService;
  private final SubscriptionBillingTimeSeriesService subscriptionBillingTimeSeriesService;
  private final SubscriptionBillingStatsHtmlRenderer subscriptionBillingStatsHtmlRenderer;

  @GetMapping(DASHBOARD_PATH)
  @ResponseBody
  public ResponseEntity<?> getSubscriptionBillingStats(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String granularity,
      @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String acceptHeader) {
    var today = LocalDate.now(PARIS_ZONE);
    var resolvedFrom = from != null ? from : today.withDayOfMonth(1);
    var resolvedTo = to != null ? to : today;
    var stats = subscriptionBillingStatsService.getStats(resolvedFrom, resolvedTo);
    if (jsonRequested(acceptHeader)) {
      return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(stats);
    }
    var resolvedGranularity = parseGranularity(granularity);
    var timeSeries =
        subscriptionBillingTimeSeriesService.getTimeSeries(
            resolvedFrom, resolvedTo, resolvedGranularity);
    var html =
        subscriptionBillingStatsHtmlRenderer.render(
            stats, timeSeries, resolvedFrom, resolvedTo, resolvedGranularity);
    return html(html);
  }

  @GetMapping(SIGNIN_PATH)
  @ResponseBody
  public ResponseEntity<String> signinPage(@RequestParam(required = false) String error) {
    return html(subscriptionBillingStatsHtmlRenderer.renderSignin(error != null));
  }

  @PostMapping(SIGNIN_PATH)
  public ResponseEntity<Void> submitSignin(
      @RequestParam(required = false) String apiKey, HttpServletRequest request) {
    if (apiKey == null || apiKey.isBlank()) {
      return ResponseEntity.status(HttpStatus.FOUND)
          .location(URI.create(SIGNIN_PATH + "?error"))
          .build();
    }
    var cookie =
        ResponseCookie.from(API_KEY_HEADER, apiKey.trim())
            .httpOnly(true)
            .secure(request.isSecure())
            .path(COOKIE_PATH)
            .sameSite("Lax")
            .build();
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .location(URI.create(DASHBOARD_PATH))
        .build();
  }

  @GetMapping("/subscriptionBillingStats/logout")
  public ResponseEntity<Void> logout(HttpServletRequest request) {
    var cookie =
        ResponseCookie.from(API_KEY_HEADER, "")
            .httpOnly(true)
            .secure(request.isSecure())
            .path(COOKIE_PATH)
            .sameSite("Lax")
            .maxAge(0)
            .build();
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .location(URI.create(SIGNIN_PATH))
        .build();
  }

  private static ResponseEntity<String> html(String body) {
    return ResponseEntity.ok()
        .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
        .body(body);
  }

  private static BillingGranularity parseGranularity(String granularity) {
    try {
      return BillingGranularity.fromNullable(granularity);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(
          "'granularity' must be one of DAY, WEEK, MONTH but was '" + granularity + "'");
    }
  }

  private static boolean jsonRequested(String acceptHeader) {
    if (acceptHeader == null || acceptHeader.isBlank()) {
      return false;
    }
    try {
      return MediaType.parseMediaTypes(acceptHeader).stream()
          .anyMatch(
              mediaType ->
                  mediaType.equalsTypeAndSubtype(MediaType.APPLICATION_JSON)
                      || mediaType.getSubtype().endsWith("+json"));
    } catch (InvalidMediaTypeException e) {
      return false;
    }
  }
}
