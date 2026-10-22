package app.bpartners.api.service.annotation.model.custompage;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CustomPage {
  private final String pageTitle;
  private final List<PageSection> sections;
}
