package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.EmailStatus;
import app.bpartners.api.model.exception.NotImplementedException;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode
public class Email implements Serializable {
  private String id;
  private String idUser;
  private List<String> recipients;
  private String object;
  private String body;
  private List<Attachment> attachments;
  private EmailStatus status;

  public String getRecipient() {
    if (recipients.size() > 1) {
      throw new NotImplementedException("Only one email recipient is supported for now");
    }
    return recipients.get(0);
  }

  public String describe() {
    return "Email(id=" + id + ", object=" + object + ",idUser=" + idUser + ")";
  }
}
