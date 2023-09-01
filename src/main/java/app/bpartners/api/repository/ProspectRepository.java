package app.bpartners.api.repository;

import app.bpartners.api.model.Prospect;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectResult;
import java.time.LocalDate;
import java.util.List;

public interface ProspectRepository {
  List<Prospect> findAllByIdAccountHolder(String idAccountHolder);

  List<Prospect> saveAll(List<Prospect> prospects);

  Prospect save(Prospect prospect);

  List<Prospect> create(List<Prospect> prospects);

  boolean needsProspects(String idAccountHolder, LocalDate date);

  boolean isSogefiProspector(String idAccountHolder);

  List<ProspectResult> evaluate(List<ProspectEval> prospectEvals);
}
