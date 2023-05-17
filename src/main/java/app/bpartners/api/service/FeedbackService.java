package app.bpartners.api.service;

import app.bpartners.api.model.Feedback;
import app.bpartners.api.repository.FeedBackRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FeedbackService {
  private final FeedBackRepository repository;

  public Feedback save(String accountHolderId, Feedback toSave) {
    return repository.saveAll(accountHolderId, List.of(toSave)).get(0);
  }
}
