package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.subscription.BillingInterval;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSubscriptionProductJpaRepository
    extends JpaRepository<UserSubscriptionProduct, String> {
  @Query(
      "select usp from user_subscription_product usp where usp.userId = :userId"
          + " and (usp.subscriptionStartDatetime is null or usp.subscriptionStartDatetime <= :now)"
          + " and (usp.subscriptionEndDatetime is null or usp.subscriptionEndDatetime > :now)"
          + " order by usp.subscriptionStartDatetime desc")
  List<UserSubscriptionProduct> findAllActiveByUserId(
      @Param("userId") String userId, @Param("now") Instant now);

  @Query(
      "select usp from user_subscription_product usp where usp.userId = :userId"
          + " and (usp.subscriptionEndDatetime is null or usp.subscriptionEndDatetime > :now)"
          + " order by usp.subscriptionStartDatetime desc")
  List<UserSubscriptionProduct> findAllNotEndedByUserId(
      @Param("userId") String userId, @Param("now") Instant now);

  @Query(
      "select usp from user_subscription_product usp where usp.userId = :userId"
          + " and usp.subscriptionEndDatetime is null"
          + " order by usp.subscriptionStartDatetime desc")
  List<UserSubscriptionProduct> findAllNotCancelledByUserId(@Param("userId") String userId);

  @Query(
      "select distinct usp.userId from user_subscription_product usp where"
          + " (usp.subscriptionStartDatetime is null or usp.subscriptionStartDatetime <= :now) and"
          + " (usp.subscriptionEndDatetime is null or usp.subscriptionEndDatetime > :now)")
  List<String> findUserIdsWithActiveSubscriptionProduct(@Param("now") Instant now);

  @Query(
      "select distinct usp.userId from user_subscription_product usp where"
          + " usp.billingInterval = :billingInterval and"
          + " (usp.subscriptionStartDatetime is null or usp.subscriptionStartDatetime <= :now) and"
          + " (usp.subscriptionEndDatetime is null or usp.subscriptionEndDatetime > :now)")
  List<String> findUserIdsWithActiveSubscriptionProductByInterval(
      @Param("now") Instant now, @Param("billingInterval") BillingInterval billingInterval);
}
