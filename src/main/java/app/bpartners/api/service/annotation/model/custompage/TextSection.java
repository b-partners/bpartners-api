package app.bpartners.api.service.annotation.model.custompage;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class TextSection extends PageSection {
  private final String text;

  @Builder
  public TextSection(SectionPriority priority, String text) {
    super(SectionType.TEXT, priority);
    this.text = text;
  }
}
