package app.bpartners.api.service.annotation.model.custompage;

import static app.bpartners.api.service.annotation.model.custompage.SectionType.SPLIT_SECTION;

import java.util.List;
import lombok.Getter;

/** Sections laid out side by side; the column count drives the CSS layout (split-2, split-3). */
@Getter
public class SplitSection extends PageSection {
  private final List<PageSection> columns;

  public SplitSection(SectionPriority priority, List<PageSection> columns) {
    super(SPLIT_SECTION, priority);
    this.columns = columns;
  }
}
