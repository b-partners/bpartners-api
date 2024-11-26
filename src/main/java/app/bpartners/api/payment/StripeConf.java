package app.bpartners.api.payment;

import com.stripe.Stripe;
import com.stripe.StripeClient;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class StripeConf {
  private static final String EURO_ISO_CURRENCY_CODE = "EUR";
  private final String apiKey;
  private final String essentialSubscriptionProductId;

  public StripeConf(@Value("${stripe.private.api.key}") String apiKey,
                    @Value("${stripe.subscription.product.essential.id}") String essentialSubscriptionProductId) {
    this.apiKey = apiKey;
      this.essentialSubscriptionProductId = essentialSubscriptionProductId;
      Stripe.apiKey = apiKey;
  }

  @Bean
  public StripeClient stripeClient() {
    return StripeClient.builder().setApiKey(apiKey).build();
  }

  public static String defaultCurrency() {
    return EURO_ISO_CURRENCY_CODE.toLowerCase();
  }
}
