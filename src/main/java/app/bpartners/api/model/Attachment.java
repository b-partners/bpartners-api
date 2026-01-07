package app.bpartners.api.model;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode
public class Attachment {
  private String idEmail;
  private String fileId;
  private String name;
  private byte[] content;
}
