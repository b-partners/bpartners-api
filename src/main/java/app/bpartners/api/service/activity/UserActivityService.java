package app.bpartners.api.service.activity;

import app.bpartners.api.model.activity.UserActivity;
import app.bpartners.api.repository.jpa.UserActivityJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserActivityService {
  private final UserActivityJpaRepository repository;

  public UserActivity record(UserActivity userActivity) {
    return repository.save(userActivity);
  }
}
