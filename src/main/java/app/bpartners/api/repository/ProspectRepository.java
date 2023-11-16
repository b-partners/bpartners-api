package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.ContactNature;
import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.model.prospect.Prospect;
import java.time.LocalDate;
import java.util.List;

public interface ProspectRepository {
  Prospect getById(String id);

  List<Prospect> findAllByStatus(ProspectStatus status);

  List<Prospect> findAllByIdAccountHolder(String idAccountHolder,
                                          String name,
                                          ContactNature contactNature);

  List<Prospect> saveAll(List<Prospect> prospects);

  Prospect save(Prospect prospect);

  List<Prospect> createAll(List<Prospect> prospects);

  boolean needsProspects(String idAccountHolder, LocalDate date);

  boolean isSogefiProspector(String idAccountHolder);
}
