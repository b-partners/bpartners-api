package app.bpartners.api.service.annotation.model.custompage;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class TableData {
  private final List<String> headers;
  private final List<List<String>> rows;
}
