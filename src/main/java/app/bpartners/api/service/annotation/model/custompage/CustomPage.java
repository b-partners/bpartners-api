package app.bpartners.api.service.annotation.model.custompage;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CustomPage {
  private String pageTitle;
  private List<PageSection> sections;
}
