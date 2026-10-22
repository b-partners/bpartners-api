package app.bpartners.api.service.annotation.model.custompage;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class ImageSection extends PageSection {
  private final String url;
  private final String caption;

  @Builder
  public ImageSection(SectionPriority priority, String url, String caption) {
    super(SectionType.IMAGE, priority);
    this.url = url;
    this.caption = caption;
  }
}
