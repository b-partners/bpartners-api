package app.bpartners.api.repository;

import app.bpartners.api.repository.expressif.ProspectEvaluation;
import app.bpartners.api.repository.expressif.ProspectResult;
import java.util.List;

public interface ProspectEvaluationRepository {

  List<ProspectEvaluation> findBySpreadsheet(String spreadsheetName,
                                             String sheetName,
                                             Integer minRange,
                                             Integer maxRange);

  List<ProspectResult> evaluate(List<ProspectEvaluation> prospectEvaluations);
}
