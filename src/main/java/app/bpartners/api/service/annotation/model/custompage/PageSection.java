package app.bpartners.api.service.annotation.model.custompage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class PageSection {
  private SectionType type;
  private SectionPriority priority;
}
