package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class UpdateInvoiceStatus {
  private String invoiceId;
  private ArchiveStatus status;
}
