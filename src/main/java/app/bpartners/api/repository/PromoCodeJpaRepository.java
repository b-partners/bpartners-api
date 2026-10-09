package app.bpartners.api.repository;

import app.bpartners.api.model.PromoCode;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromoCodeJpaRepository extends JpaRepository<PromoCode, String> {
  Optional<PromoCode> findByCode(String code);

  Optional<PromoCode> findByCodeAndDeprecatedFalse(String code);
}
