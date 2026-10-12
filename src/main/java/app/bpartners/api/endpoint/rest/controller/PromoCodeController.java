package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.PromoCodeRestMapper;
import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.endpoint.rest.model.PromoCode;
import app.bpartners.api.endpoint.rest.model.PromoCodeStats;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.service.user.PromoCodeService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class PromoCodeController {
  private final PromoCodeService promoCodeService;
  private final PromoCodeRestMapper promoCodeMapper;
  private final UserRestMapper userMapper;

  @GetMapping("/promoCodes/{code}")
  public PromoCode checkPromoCode(@PathVariable String code) {
    var promoCode =
        promoCodeService
            .findActiveByCode(code)
            .orElseThrow(() -> new NotFoundException("PromoCode(code=" + code + ") not found"));
    return promoCodeMapper.toRest(promoCode);
  }

  @GetMapping("/promoCodes")
  public List<PromoCodeStats> getPromoCodes() {
    return promoCodeService.getStats().stream().map(promoCodeMapper::toRest).toList();
  }

  @GetMapping("/promoCodes/{code}/users")
  public List<User> getPromoCodeUsers(@PathVariable String code) {
    return promoCodeService.getUsersByCode(code).stream().map(userMapper::toRest).toList();
  }
}
