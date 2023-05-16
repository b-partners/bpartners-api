package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HAccountHolder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountHolderJpaRepository extends JpaRepository<HAccountHolder, String> {
  List<HAccountHolder> findAllByAccountId(String accountId);
  Optional<HAccountHolder> findByAccountId(String accountId);

  Optional<HAccountHolder> findByIdAndAccountId(String id, String accountId);

  @Query(value =
      "select new HAccountHolder (ac.id, ac.accountId, ac.socialCapital, ac.vatNumber,"
          + " ac.mobilePhoneNumber, ac.email,"
          + " ac.subjectToVat,ac.initialCashflow, ac.verificationStatus,"
          + "ac.name, ac.registrationNumber, ac.businessActivity,"
          + "ac.businessActivityDescription, ac.feedbackLink, ac.address, ac.city, ac.country, ac.postalCode"
          + ",ac.longitude, ac.latitude, ac.townCode, ac.prospectingPerimeter)"
          + " from HAccountHolder ac group by ac.accountId, ac.id,"
          + " ac.socialCapital, ac.vatNumber, ac.mobilePhoneNumber, ac.email, ac.initialCashflow, ac.feedbackLink")
  List<HAccountHolder> findAllGroupByAccountId();
}