package app.bpartners.api.model;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@ToString
public class Feedback {
  private String id;
  private AccountHolder accountHolder;
  private List<Customer> customers;
  private Instant creationDatetime;
  private String subject;
  private String message;
}
