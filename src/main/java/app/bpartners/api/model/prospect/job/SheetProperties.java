package app.bpartners.api.model.prospect.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class SheetProperties {
  private String spreadsheetName;
  private String sheetName;
  private SheetRange ranges; //TODO: set to Collections
}