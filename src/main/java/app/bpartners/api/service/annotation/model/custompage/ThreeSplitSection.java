package app.bpartners.api.service.annotation.model.custompage;

import static app.bpartners.api.service.annotation.model.custompage.SectionType.THREE_SPLIT_SECTION;

import lombok.Getter;

@Getter
public class ThreeSplitSection extends PageSection {
  private final PageSection leftSection;
  private final PageSection middleSection;
  private final PageSection rightSection;

  public ThreeSplitSection(
      SectionPriority priority,
      PageSection leftSection,
      PageSection middleSection,
      PageSection rightSection) {
    super(THREE_SPLIT_SECTION, priority);
    this.leftSection = leftSection;
    this.middleSection = middleSection;
    this.rightSection = rightSection;
  }
}
