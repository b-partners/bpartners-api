package app.bpartners.api.service.annotation.model.custompage;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public abstract class PageSection {
  protected SectionType type;
  protected SectionPriority priority;

  public static PageSection fromRest(app.bpartners.api.endpoint.rest.model.PageSection rest) {
    SectionPriority priority = SectionPriority.valueOf(rest.getPriority().name());

    switch (rest) {
      case app.bpartners.api.endpoint.rest.model.TextSection textRestSection -> {
        return TextSection.builder().priority(priority).text(textRestSection.getText()).build();
      }
      case app.bpartners.api.endpoint.rest.model.ImageSection imageRestSection -> {
        return ImageSection.builder()
            .priority(priority)
            .url(Optional.of(imageRestSection.getUrl()).map(Object::toString).orElse(null))
            .caption(imageRestSection.getCaption())
            .build();
      }
      case app.bpartners.api.endpoint.rest.model.TableSection tableRestSection -> {
        app.bpartners.api.endpoint.rest.model.TableData restTableData =
            tableRestSection.getTableData();
        return TableSection.builder()
            .priority(priority)
            .tableData(
                TableData.builder()
                    .headers(
                        Optional.of(restTableData)
                            .map(app.bpartners.api.endpoint.rest.model.TableData::getHeaders)
                            .orElse(java.util.List.of()))
                    .rows(
                        Optional.of(restTableData)
                            .map(app.bpartners.api.endpoint.rest.model.TableData::getRows)
                            .orElse(java.util.List.of()))
                    .build())
            .build();
      }
      case app.bpartners.api.endpoint.rest.model.SplitSection splitRestSection -> {
        return new SplitSection(
            priority,
            fromRest(splitRestSection.getLeftSection()),
            fromRest(splitRestSection.getRightSection()));
      }
      case app.bpartners.api.endpoint.rest.model.ThreeSplitSection threeSplitRestSection -> {
        return new ThreeSplitSection(
            priority,
            fromRest(threeSplitRestSection.getLeftSection()),
            fromRest(threeSplitRestSection.getMiddleSection()),
            fromRest(threeSplitRestSection.getRightSection()));
      }
      default -> throw new IllegalArgumentException("Unknown section type: " + rest.getClass());
    }
  }
}
