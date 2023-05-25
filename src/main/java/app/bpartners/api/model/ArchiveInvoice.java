package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ArchiveInvoice {
  private String idInvoice;
  private ArchiveStatus status;
}
