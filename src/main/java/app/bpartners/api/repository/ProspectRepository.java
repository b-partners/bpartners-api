package app.bpartners.api.repository;

import app.bpartners.api.model.Prospect;
import java.util.List;

public interface ProspectRepository {
  List<Prospect> findAllByIdAccountHolder(String idAccountHolder);

  List<Prospect> saveAll(List<Prospect> prospects);
}
