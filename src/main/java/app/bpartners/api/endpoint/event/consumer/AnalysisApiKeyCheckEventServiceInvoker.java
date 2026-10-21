package app.bpartners.api.endpoint.event.consumer;

import app.bpartners.api.endpoint.event.consumer.model.TypedEvent;
import app.bpartners.api.endpoint.event.model.AllAnalysisApiKeyCheckTriggered;
import app.bpartners.api.endpoint.event.model.AnalysisApiKeyCheckTriggered;
import app.bpartners.api.endpoint.event.model.PojaEvent;
import app.bpartners.api.service.event.AllAnalysisApiKeyCheckTriggeredService;
import app.bpartners.api.service.event.AnalysisApiKeyCheckTriggeredService;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Same as {@link EventServiceInvoker} but skips its ~10s per event classpath scan for the analysis
 * api key check events, other events fall back to it.
 */
@Primary
@Component
public class AnalysisApiKeyCheckEventServiceInvoker extends EventServiceInvoker {
  private static final Map<String, Class<? extends Consumer<?>>> SERVICES =
      Map.of(
          AnalysisApiKeyCheckTriggered.class.getTypeName(),
          AnalysisApiKeyCheckTriggeredService.class,
          AllAnalysisApiKeyCheckTriggered.class.getTypeName(),
          AllAnalysisApiKeyCheckTriggeredService.class);

  private final ApplicationContext applicationContext;

  public AnalysisApiKeyCheckEventServiceInvoker(ApplicationContext applicationContext) {
    super(applicationContext);
    this.applicationContext = applicationContext;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void accept(TypedEvent typedEvent) {
    var service = SERVICES.get(typedEvent.typeName());
    if (service == null) {
      super.accept(typedEvent);
      return;
    }
    ((Consumer<PojaEvent>) applicationContext.getBean(service)).accept(typedEvent.payload());
  }
}
