package app.bpartners.api.service.annotation.model.custompage;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class TableSection extends PageSection {
  private final TableData tableData;

  @Builder
  public TableSection(SectionPriority priority, TableData tableData) {
    super(SectionType.TABLE, priority);
    this.tableData = tableData;
  }
}
