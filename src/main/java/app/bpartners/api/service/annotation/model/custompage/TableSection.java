package app.bpartners.api.service.annotation.model.custompage;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TableSection extends PageSection {
  private TableData tableData;

  @Builder
  public TableSection(SectionPriority priority, TableData tableData) {
    super(SectionType.TABLE, priority);
    this.tableData = tableData;
  }
}
