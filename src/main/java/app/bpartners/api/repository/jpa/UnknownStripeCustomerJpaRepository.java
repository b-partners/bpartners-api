package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.UnknownStripeCustomer;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnknownStripeCustomerJpaRepository
    extends JpaRepository<UnknownStripeCustomer, String> {

  List<UnknownStripeCustomer> findAllByCreationDatetimeBetween(Instant begin, Instant end);
}
