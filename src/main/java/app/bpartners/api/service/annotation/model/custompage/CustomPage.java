package app.bpartners.api.service.annotation.model.custompage;

import static java.util.stream.Collectors.toUnmodifiableList;

import java.util.List;
import java.util.Optional;
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

  public static CustomPage fromRest(app.bpartners.api.endpoint.rest.model.CustomPage rest) {
    return CustomPage.builder()
        .pageTitle(rest.getPageTitle())
        .sections(
            Optional.ofNullable(rest.getSections()).orElse(List.of()).stream()
                .map(PageSection::fromRest)
                .collect(toUnmodifiableList()))
        .build();
  }
}
