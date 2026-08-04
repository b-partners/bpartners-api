package app.bpartners.api.service.annotation.model.custompage;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageSection extends PageSection {
  private String url;
  private String caption;

  @Builder
  public ImageSection(SectionPriority priority, String url, String caption) {
    super(SectionType.IMAGE, priority);
    this.url = url;
    this.caption = caption;
  }
}
