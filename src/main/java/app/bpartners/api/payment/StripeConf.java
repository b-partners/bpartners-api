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
  private final String basicSubscriptionProductId;
  private final String webhookSecret;

  public StripeConf(
      @Value("${stripe.private.api.key}") String apiKey,
      @Value("${stripe.subscription.product.essential.id}") String essentialSubscriptionProductId,
      @Value("${stripe.subscription.product.basic.id}") String basicSubscriptionProductId,
      @Value("${stripe.webhook.secret}") String webhookSecret) {
    this.apiKey = apiKey;
    this.essentialSubscriptionProductId = essentialSubscriptionProductId;
    this.basicSubscriptionProductId = basicSubscriptionProductId;
    this.webhookSecret = webhookSecret;
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
