package app.bpartners.api.service.prospect.relaunch;

import static app.bpartners.api.endpoint.rest.model.ProspectStatus.TO_CONTACT;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.service.prospect.ProspectStatusService;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProspectToRelaunchFinder {
  private final ProspectStatusService statusService;

  public Map<String, List<Prospect>> findGroupedByHolder() {
    return statusService.findAllByStatus(TO_CONTACT).stream()
        .filter(ProspectToRelaunchFinder::isToRelaunch)
        .collect(groupingBy(Prospect::getIdHolderOwner, TreeMap::new, toList()));
  }

  private static boolean isToRelaunch(Prospect prospect) {
    return prospect.getIdHolderOwner() != null && isRated(prospect);
  }

  private static boolean isRated(Prospect prospect) {
    var rating = prospect.getRating();
    return rating != null && rating.getValue() != null && rating.getValue() > 0;
  }
}
