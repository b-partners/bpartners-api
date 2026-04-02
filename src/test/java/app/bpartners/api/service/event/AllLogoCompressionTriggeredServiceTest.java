package app.bpartners.api.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.AllLogoCompressionTriggered;
import app.bpartners.api.endpoint.event.model.LogoCompressionTriggered;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class AllLogoCompressionTriggeredServiceTest {

  UserRepository userRepository = mock();
  EventProducer eventProducer = mock();
  AllLogoCompressionTriggeredService subject =
      new AllLogoCompressionTriggeredService(userRepository, eventProducer);

  @Test
  void accept() {
    var event = new AllLogoCompressionTriggered();
    var allUsers = allUsers();
    when(userRepository.findAll()).thenReturn(allUsers);
    doNothing().when(eventProducer).accept(anyList());

    subject.accept(event);
    verify(eventProducer).accept(allUsers.stream().map(this::toTypedEvent).toList());
  }

  private LogoCompressionTriggered toTypedEvent(User user) {
    return new LogoCompressionTriggered(user.getId(), user.getLogoFileId());
  }

  private List<User> allUsers() {
    return List.of(user(), user(), user(), user());
  }

  private User user() {
    return new User()
        .toBuilder().id(randomUUID().toString()).logoFileId(randomUUID().toString()).build();
  }
}
