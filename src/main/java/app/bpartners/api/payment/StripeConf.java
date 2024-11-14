package app.bpartners.api.payment;

import com.stripe.StripeClient;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class StripeConf {
  private final String apiKey;

  public StripeConf(@Value("${stripe.private.api.key}") String apiKey) {
    this.apiKey = apiKey;
  }

  @Bean
  public StripeClient stripeClient() {
    return StripeClient.builder().setApiKey(apiKey).build();
  }
}
