package app.bpartners.api.service;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.expressif.ProspectEval;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import static app.bpartners.api.model.prospect.job.SheetEvaluationJobRunner.GOLDEN_SOURCE_SPR_SHEET_NAME;

@Service
@AllArgsConstructor
public class ProspectScheduleService {
  public static final int MIN_DEFAULT_RANGE = 2;
  public static final int MAX_DEFAULT_RANGE = 100;
  private final UserService userService;
  private final ProspectService prospectService;
  private final ProspectEvaluationService prospectEvaluationService;

  //@Scheduled(fixedRate = 15 * 60 * 1_000)
  void importProspectsFromSheet() {
    List<User> users = userService.findAll();
    for (User u : users) {
      AccountHolder accountHolder = u.getDefaultHolder();
      String sheetName = accountHolder.getName();
      List<ProspectEval> sheetsProspectEvaluations =
          prospectService.readEvaluationsFromSheetsWithoutFilter(
              u.getId(),
              accountHolder.getId(),
              GOLDEN_SOURCE_SPR_SHEET_NAME,
              sheetName,
              MIN_DEFAULT_RANGE,
              MAX_DEFAULT_RANGE);

      //TODO: check if prospects already imported then filter only new prospects
      List<ProspectEval> newProspects = sheetsProspectEvaluations;

      prospectEvaluationService.saveAll(newProspects);
    }
  }
}
