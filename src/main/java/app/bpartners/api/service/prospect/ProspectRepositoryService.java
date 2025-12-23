package app.bpartners.api.service.prospect;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.ProspectUpdated;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.ProspectRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProspectRepositoryService {
  private final ProspectRepository repository;
  private final EventProducer eventProducer;

  public List<Prospect> saveAll(List<Prospect> toSave) {
    var savedProspects = repository.saveAll(toSave);

    savedProspects.forEach(
        savedProspect -> {
          var optionalProspect =
              toSave.stream()
                  .filter(
                      prospect -> savedProspect.getEmail().equalsIgnoreCase(prospect.getEmail()))
                  .findFirst();
          eventProducer.accept(
              List.of(
                  ProspectUpdated.builder()
                      .prospect(savedProspect)
                      .isNew(optionalProspect.map(Prospect::isNew).orElse(false))
                      .updatedAt(Instant.now())
                      .build()));
        });

    return savedProspects;
  }
}
