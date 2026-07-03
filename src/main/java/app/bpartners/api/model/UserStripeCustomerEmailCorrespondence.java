package app.bpartners.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity(name = "user_stripe_customer_email_correspondence")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class UserStripeCustomerEmailCorrespondence {
  @Id private String id;
  private String userId;
  private String stripeCustomerId;
  private String email;
}
