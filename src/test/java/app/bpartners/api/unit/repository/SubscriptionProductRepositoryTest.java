package app.bpartners.api.unit.repository;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SubscriptionProductRepositoryTest extends MockedThirdParties {

  private static final String MOST_CHOSEN_PLAN_ID = "c5f57306-a7b1-43f4-90fc-204ccd4c0ce2";
  private static final String NOT_MOST_CHOSEN_PLAN_ID = "89f1acdd-c3b9-4717-a21d-355b2021ad58";

  @Autowired private SubscriptionProductRepository subject;

  @Test
  void reads_most_chosen_flag_set_by_migration() {
    var mostChosenPlan = subject.findById(MOST_CHOSEN_PLAN_ID).orElseThrow();

    assertTrue(mostChosenPlan.isMostChosen());
  }

  @Test
  void reads_most_chosen_flag_defaulting_to_false() {
    var otherPlan = subject.findById(NOT_MOST_CHOSEN_PLAN_ID).orElseThrow();

    assertFalse(otherPlan.isMostChosen());
  }

  @Test
  void persists_and_reads_back_most_chosen_flag() {
    var id = randomUUID().toString();
    subject.save(
        SubscriptionProduct.builder().id(id).name("Persisted plan").mostChosen(true).build());

    var reloaded = subject.findById(id).orElseThrow();

    assertTrue(reloaded.isMostChosen());
  }
}
