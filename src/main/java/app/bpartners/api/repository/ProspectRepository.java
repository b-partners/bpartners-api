package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.ContactNature;
import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectResult;
import java.time.LocalDate;
import java.util.List;

public interface ProspectRepository {
  Prospect getById(String id);

  List<Prospect> findAllByStatus(ProspectStatus status);

  List<Prospect> findAllByIdAccountHolder(
      String idAccountHolder, String name, ContactNature contactNature);

  List<Prospect> saveAll(List<Prospect> prospects);

  Prospect save(Prospect prospect);

  List<Prospect> create(List<Prospect> prospects);

  boolean needsProspects(String idAccountHolder, LocalDate date);

  boolean isSogefiProspector(String idAccountHolder);

  List<ProspectResult> evaluate(List<ProspectEval> prospectEvals);
}
