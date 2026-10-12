package app.bpartners.api.service.user;

import app.bpartners.api.model.PromoCode;
import app.bpartners.api.model.PromoCodeStats;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.PromoCodeJpaRepository;
import app.bpartners.api.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PromoCodeService {
  private final PromoCodeJpaRepository promoCodeJpaRepository;
  private final UserRepository userRepository;

  public Optional<PromoCode> findActiveByCode(String code) {
    return promoCodeJpaRepository.findByCodeAndDeprecatedFalse(code);
  }

  public String resolveId(String code) {
    if (code == null || code.isBlank()) {
      return null;
    }
    return findActiveByCode(code).map(PromoCode::getId).orElse(null);
  }

  public List<PromoCodeStats> getStats() {
    return promoCodeJpaRepository.findAll().stream()
        .map(
            promoCode ->
                PromoCodeStats.builder()
                    .promoCode(promoCode)
                    .registeredCount(userRepository.countByPromoCodeId(promoCode.getId()))
                    .build())
        .toList();
  }

  public List<User> getUsersByCode(String code) {
    PromoCode promoCode = getByCode(code);
    return userRepository.findAllByPromoCodeId(promoCode.getId());
  }

  public PromoCode getByCode(String code) {
    return promoCodeJpaRepository
        .findByCode(code)
        .orElseThrow(() -> new NotFoundException("PromoCode(code=" + code + ") not found"));
  }
}
