package app.bpartners.api.service.annotation.model.custompage;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract sealed class PageSection
    permits TextSection, ImageSection, TableSection, SplitSection {
  private final SectionType type;
  private final SectionPriority priority;
}
