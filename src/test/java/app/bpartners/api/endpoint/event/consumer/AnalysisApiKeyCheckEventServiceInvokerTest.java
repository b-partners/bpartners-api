package app.bpartners.api.endpoint.event.consumer;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.concurrency.Workers;
import app.bpartners.api.endpoint.event.consumer.model.TypedEvent;
import app.bpartners.api.endpoint.event.model.AllAnalysisApiKeyCheckTriggered;
import app.bpartners.api.endpoint.event.model.AnalysisApiKeyCheckTriggered;
import app.bpartners.api.service.event.AllAnalysisApiKeyCheckTriggeredService;
import app.bpartners.api.service.event.AnalysisApiKeyCheckTriggeredService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

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

  @Test
  void event_consumer_gets_the_dedicated_invoker_injected() {
    try (var context =
        new AnnotationConfigApplicationContext(
            Workers.class,
            EventServiceInvoker.class,
            AnalysisApiKeyCheckEventServiceInvoker.class,
            EventConsumer.class)) {
      var consumer = context.getBean(EventConsumer.class);

      assertInstanceOf(
          AnalysisApiKeyCheckEventServiceInvoker.class,
          ReflectionTestUtils.getField(consumer, "eventServiceInvoker"));
    }
  }
}
