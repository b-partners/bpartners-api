package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.AllLogoCompressionTriggered;
import app.bpartners.api.endpoint.event.model.LogoCompressionTriggered;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.UserRepository;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AllLogoCompressionTriggeredService implements Consumer<AllLogoCompressionTriggered> {
  private final UserRepository userRepository;
  private final EventProducer eventProducer;

  @Override
  public void accept(AllLogoCompressionTriggered allLogoCompressionTriggered) {
    List<User> allUsers = userRepository.findAll();
    List<LogoCompressionTriggered> events = allUsers.stream().map(this::toTypedEvent).toList();

    eventProducer.accept(events);
  }

  private LogoCompressionTriggered toTypedEvent(User user) {
    return new LogoCompressionTriggered(user.getId(), user.getLogoFileId());
  }
}
