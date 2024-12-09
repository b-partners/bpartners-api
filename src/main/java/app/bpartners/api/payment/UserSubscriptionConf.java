package app.bpartners.api.payment;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class UserSubscriptionConf {
  private final String userToCreditId;

  public UserSubscriptionConf(@Value("${subscription.user.credit.id}") String userToCreditId) {
    this.userToCreditId = userToCreditId;
  }
}
