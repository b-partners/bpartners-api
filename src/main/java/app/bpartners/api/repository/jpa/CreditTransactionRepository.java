package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.credit.CreditTransactionType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, String> {
  List<CreditTransaction> findAllByUserId(String userId);

  Optional<CreditTransaction> findFirstByCreditPurchaseId(String creditPurchaseId);

  boolean existsByUserIdAndTypeAndSubscriptionProductIdAndGrantPeriodStart(
      String userId,
      CreditTransactionType type,
      String subscriptionProductId,
      LocalDate grantPeriodStart);

  List<CreditTransaction> findAllByUserIdAndTypeAndSubscriptionProductIdIsNull(
      String userId, CreditTransactionType type);

  @Query(
      value = "select 1 from (select pg_advisory_xact_lock(hashtext(:userId), 0)) as lock_acquired",
      nativeQuery = true)
  Integer acquireWalletLock(@Param("userId") String userId);

  List<CreditTransaction> findByUserIdAndCreationDatetimeBetweenOrderByCreationDatetimeDesc(
      String userId, Instant from, Instant to, Pageable pageable);

  List<CreditTransaction>
      findByUserIdAndTypeInAndCreationDatetimeBetweenOrderByCreationDatetimeDesc(
          String userId,
          List<CreditTransactionType> types,
          Instant from,
          Instant to,
          Pageable pageable);

  @Query(
      "select ct from credit_transaction ct"
          + " where ct.type in :types"
          + " and ct.creationDatetime >= :from and ct.creationDatetime < :to")
  List<CreditTransaction> findByTypesBetween(
      @Param("types") List<CreditTransactionType> types,
      @Param("from") Instant from,
      @Param("to") Instant to);
}
