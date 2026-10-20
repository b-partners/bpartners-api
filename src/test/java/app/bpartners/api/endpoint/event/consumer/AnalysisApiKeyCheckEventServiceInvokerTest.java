package app.bpartners.api.endpoint.event.consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.consumer.model.TypedEvent;
import app.bpartners.api.endpoint.event.model.AllAnalysisApiKeyCheckTriggered;
import app.bpartners.api.endpoint.event.model.AnalysisApiKeyCheckTriggered;
import app.bpartners.api.service.event.AllAnalysisApiKeyCheckTriggeredService;
import app.bpartners.api.service.event.AnalysisApiKeyCheckTriggeredService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

class AnalysisApiKeyCheckEventServiceInvokerTest {
  ApplicationContext applicationContext = mock();
  AnalysisApiKeyCheckEventServiceInvoker subject =
      new AnalysisApiKeyCheckEventServiceInvoker(applicationContext);

  @Test
  void accept_invokes_check_service() {
    var service = mock(AnalysisApiKeyCheckTriggeredService.class);
    when(applicationContext.getBean(AnalysisApiKeyCheckTriggeredService.class)).thenReturn(service);
    var event = new AnalysisApiKeyCheckTriggered("user");

    subject.accept(new TypedEvent(AnalysisApiKeyCheckTriggered.class.getTypeName(), event));

    verify(service).accept(event);
  }

  @Test
  void accept_invokes_all_check_service() {
    var service = mock(AllAnalysisApiKeyCheckTriggeredService.class);
    when(applicationContext.getBean(AllAnalysisApiKeyCheckTriggeredService.class))
        .thenReturn(service);
    var event = new AllAnalysisApiKeyCheckTriggered();

    subject.accept(new TypedEvent(AllAnalysisApiKeyCheckTriggered.class.getTypeName(), event));

    verify(service).accept(event);
  }
}
