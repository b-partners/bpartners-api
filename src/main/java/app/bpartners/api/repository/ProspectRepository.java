package app.bpartners.api.repository;

import app.bpartners.api.expressif.ProspectEval;
import app.bpartners.api.expressif.ProspectResult;
import app.bpartners.api.model.Prospect;
import java.time.LocalDate;
import java.util.List;

public interface ProspectRepository {
  List<Prospect> findAllByIdAccountHolder(String idAccountHolder);

  List<Prospect> saveAll(List<Prospect> prospects);

  boolean needsProspects(String idAccountHolder, LocalDate date);

  boolean isSogefiProspector(String idAccountHolder);

  List<ProspectResult> evaluate(List<ProspectEval> prospectEvals);
}
