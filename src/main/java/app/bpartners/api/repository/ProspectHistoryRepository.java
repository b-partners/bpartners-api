package app.bpartners.api.repository;

import app.bpartners.api.model.ProspectHistory;
import java.util.List;

public interface ProspectHistoryRepository {
  List<ProspectHistory> getAllByIdProspect(String idProspect);
  ProspectHistory getLatestUpdateByIdProspect(String idProspect);
  List<ProspectHistory> saveAll(List<ProspectHistory> toSave);
}
