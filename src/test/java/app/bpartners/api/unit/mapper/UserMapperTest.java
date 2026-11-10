package app.bpartners.api.unit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.mapper.AccountHolderMapper;
import app.bpartners.api.model.mapper.AccountMapper;
import app.bpartners.api.model.mapper.UserApiKeyMapper;
import app.bpartners.api.model.mapper.UserMapper;
import app.bpartners.api.repository.jpa.model.HUser;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserMapperTest {
  UserMapper subject =
      new UserMapper(
          mock(AccountMapper.class), mock(AccountHolderMapper.class), mock(UserApiKeyMapper.class));

  @Test
  void to_entity_keeps_user_subscription_products() {
    var product = UserSubscriptionProduct.builder().id("product_id").userId("user_id").build();
    var user = User.builder().id("user_id").subscriptionProducts(List.of(product)).build();

    var actual = subject.toEntity(user);

    assertEquals(List.of(product), actual.getUserSubscriptionProducts());
  }

  @Test
  void to_domain_then_to_entity_round_trips_user_subscription_products() {
    var product = UserSubscriptionProduct.builder().id("product_id").userId("user_id").build();
    var entity =
        HUser.builder()
            .id("user_id")
            .roles(new app.bpartners.api.endpoint.rest.security.model.Role[] {})
            .analysisApiKeys(List.of())
            .userSubscriptionProducts(List.of(product))
            .build();

    var actual = subject.toEntity(subject.toDomain(entity));

    assertEquals(List.of(product), actual.getUserSubscriptionProducts());
  }
}
