package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.model.ProspectHistory;
import app.bpartners.api.model.mapper.ProspectHistoryMapper;
import app.bpartners.api.repository.ProspectHistoryRepository;
import app.bpartners.api.repository.jpa.model.HProspect;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ProspectHistoryService {
  private final ProspectHistoryRepository repository;
  private final ProspectHistoryMapper mapper;

  public List<ProspectHistory> saveAll(HashMap<String, ProspectStatus> toSave,
                                       String idAccountHolder) {
    return repository.saveAll(toSave.entrySet().stream()
        .map(history -> mapper.toDomain(history.getKey(), history.getValue(), idAccountHolder))
        .collect(Collectors.toList()));
  }

  public ProspectHistory save(ProspectHistory toSave) {
    return repository.save(toSave);
  }

  public ProspectHistory getLatestHistoryByIdProspect(String idProspect) {
    return repository.getLatestUpdateByIdProspect(idProspect);
  }

  public List<ProspectHistory> createDefaultHistoryForNewProspects(List<HProspect> newProspects,
                                                                   String idAccountHolder) {
    List<ProspectHistory> histories = newProspects.stream()
        .map(prospect -> ProspectHistory.builder()
            .idProspect(prospect.getId())
            .idAccountHolder(idAccountHolder)
            .status(ProspectStatus.TO_CONTACT)
            .updatedAt(Instant.now())
            .build())
        .collect(Collectors.toList());
    return repository.saveAll(histories);
  }
}
