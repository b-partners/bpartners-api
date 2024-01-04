package app.bpartners.api.endpoint.event.gen;

import app.bpartners.api.model.Customer;
import app.bpartners.api.model.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import javax.annotation.processing.Generated;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Generated("EventBridge")
@Data
@EqualsAndHashCode
@ToString
public class CustomerCrupdated implements Serializable {
  private static final long serialVersionUID = 1L;

  @JsonProperty("subject")
  private String subject;

  @JsonProperty("recipient")
  private String recipientEmail;

  @JsonProperty("user")
  private User user;

  @JsonProperty("customer")
  private Customer customer;

  @JsonProperty("type")
  private Type type;

  public CustomerCrupdated subject(String subject) {
    this.subject = subject;
    return this;
  }

  public CustomerCrupdated recipientEmail(String recipientEmail) {
    this.recipientEmail = recipientEmail;
    return this;
  }

  public CustomerCrupdated user(User user) {
    this.user = user;
    return this;
  }

  public CustomerCrupdated customer(Customer customer) {
    this.customer = customer;
    return this;
  }

  public CustomerCrupdated type(Type type) {
    this.type = type;
    return this;
  }

  public enum Type {
    UPDATE,
    CREATE
  }
}
