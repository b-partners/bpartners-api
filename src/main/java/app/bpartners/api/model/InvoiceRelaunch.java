package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.RelaunchType;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvoiceRelaunch {
  private String id;
  private RelaunchType type;
  private Invoice invoice;
  private String emailObject;
  private String emailBody;
  private boolean isUserRelaunched;
  private Instant creationDatetime;
  private String attachmentFileId;
  private List<Attachment> attachments;
}
