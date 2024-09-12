package app.bpartners.api.unit.service;

import static app.bpartners.api.integration.conf.utils.TestUtils.USER1_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.IntegratingApplication;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.service.UserService;
import app.bpartners.api.service.WhoisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class WhoisServiceTest {
  UserService userServiceMock = mock(UserService.class);
  WhoisService subject = new WhoisService(userServiceMock);

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(subject, "FEATURE_DETECTOR_API_KEY", "valid-api-key");
    ReflectionTestUtils.setField(subject, "FEATURE_DETECTOR_APPLICATION", "test-application");
  }

  @Test
  void get_specified_user() {
    var application = IntegratingApplication.builder().applicationName("").build();
    var user = User.builder().id(USER1_ID).firstName("joe").lastName("doe").build();
    when(userServiceMock.getUserById(any())).thenReturn(user);

    assertEquals(user, subject.getSpecifiedUser(application, USER1_ID));
  }

  @Test
  void validate_api_key_should_return_integrating_application_when_apiKey_is_valid() {
    var application = subject.validateApiKey("valid-api-key");

    assertEquals("test-application", application.getApplicationName());
    assertEquals("valid-api-key", application.getApiKey());
  }

  @Test
  void validate_apiKey_should_throw_forbidden_exception_when_api_key_is_invalid() {
    assertThrows(ForbiddenException.class, () -> subject.validateApiKey("invalid-api-key"));
  }

  @Test
  void validateApiKeyShouldThrowForbiddenExceptionWhenApiKeyIsNull() {
    assertThrows(ForbiddenException.class, () -> subject.validateApiKey(null));
  }

  @Test
  void validateApiKeyShouldThrowForbiddenExceptionWhenApiKeyIsEmpty() {
    assertThrows(ForbiddenException.class, () -> subject.validateApiKey(""));
  }
}
