package app.bpartners.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import lombok.*;

@Entity(name = "unknown_stripe_customer")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@ToString
public class UnknownStripeCustomer {
  @Id private String id;

  @Column(name = "stripe_customer_id")
  private String stripeCustomerIdentifier;

  private String name;
  private String email;
  private String phone;
  private String address;
  private Instant creationDatetime;
}
