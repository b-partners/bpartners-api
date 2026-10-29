package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.SecondaryUserStatus.ACTIVE;

import app.bpartners.api.model.subscription.SecondaryUser;
import app.bpartners.api.repository.jpa.SecondaryUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PrimaryUserResolver {
  private final SecondaryUserRepository secondaryUserRepository;

  public String resolve(String userId) {
    return secondaryUserRepository
        .findFirstBySecondaryUserIdAndStatus(userId, ACTIVE)
        .map(SecondaryUser::getPrimaryUserId)
        .orElse(userId);
  }

  public boolean isSecondary(String userId) {
    return !resolve(userId).equals(userId);
  }
}
