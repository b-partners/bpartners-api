package app.bpartners.api.service.annotation.model.custompage;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TextSection extends PageSection {
  private String text;

  @Builder
  public TextSection(SectionPriority priority, String text) {
    super(SectionType.TEXT, priority);
    this.text = text;
  }
}
