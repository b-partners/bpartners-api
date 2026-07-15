package app.bpartners.api.service.annotation.model.custompage;

import static app.bpartners.api.service.annotation.model.custompage.SectionType.SPLIT_SECTION;

import lombok.Getter;

@Getter
public class SplitSection extends PageSection {
  private final PageSection leftSection;
  private final PageSection rightSection;

  public SplitSection(SectionPriority priority, PageSection leftSection, PageSection rightSection) {
    super(SPLIT_SECTION, priority);
    this.leftSection = leftSection;
    this.rightSection = rightSection;
  }
}
