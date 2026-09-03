package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.EmailRecipientType;
import java.io.Serializable;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Builder(toBuilder = true)
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EmailRecipient implements Serializable {
  private String id;
  private String idAccountHolder;
  private EmailRecipientType type;
  private String email;
  private Instant updatedAt;
}
