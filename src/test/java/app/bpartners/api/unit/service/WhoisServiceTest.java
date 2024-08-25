package app.bpartners.api.unit.service;

import app.bpartners.api.model.IntegratingApplication;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.service.UserService;
import app.bpartners.api.service.WhoisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static app.bpartners.api.integration.conf.utils.TestUtils.USER1_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WhoisServiceTest {
    WhoisService subject;
    UserService userServiceMock;

    @BeforeEach
    void setUp() {
        userServiceMock = mock(UserService.class);
        subject = new WhoisService(userServiceMock);
        ReflectionTestUtils.setField(subject, "FEATURE_DETECTOR_API_KEY", "valid-api-key");
        ReflectionTestUtils.setField(subject, "FEATURE_DETECTOR_APPLICATION", "test-application");
    }

    @Test
    void get_specified_user() {
        var application = mock(IntegratingApplication.class);
        var user = mock(User.class);

        when(userServiceMock.getUserById(any())).thenReturn(user);
        when(application.getApplicationName()).thenReturn("application name");
        when(user.getName()).thenReturn("Joe Doe");
        when(user.getId()).thenReturn(USER1_ID);

        assertEquals(user, subject.getSpecifiedUser(application, USER1_ID));
    }

    @Test
    void validateApiKeyShouldReturnIntegratingApplicationWhenApiKeyIsValid() {
        var application = subject.validateApiKey("valid-api-key");

        assertEquals("test-application", application.getApplicationName());
        assertEquals("valid-api-key", application.getApiKey());
    }

    @Test
    void validateApiKeyShouldThrowForbiddenExceptionWhenApiKeyIsInvalid() {
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
