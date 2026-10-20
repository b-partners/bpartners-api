package app.bpartners.api.endpoint.event.consumer;

import app.bpartners.api.PojaGenerated;
import app.bpartners.api.endpoint.event.consumer.model.TypedEvent;
import app.bpartners.api.endpoint.event.model.AllAnalysisApiKeyCheckTriggered;
import app.bpartners.api.endpoint.event.model.AnalysisApiKeyCheckTriggered;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@PojaGenerated
@SuppressWarnings("all")
@AllArgsConstructor
@Component
@Slf4j
public class EventServiceInvoker implements Consumer<TypedEvent> {

  // the classpath scan below costs ~10s per event, these skip it (sequential check pipeline)
  private static final Set<String> DIRECT_LOOKUP_TYPES =
      Set.of(
          AnalysisApiKeyCheckTriggered.class.getTypeName(),
          AllAnalysisApiKeyCheckTriggered.class.getTypeName());

  private final ApplicationContext applicationContext;

  @SneakyThrows
  @Override
  public void accept(TypedEvent typedEvent) {
    var typeName = typedEvent.typeName();
    if (DIRECT_LOOKUP_TYPES.contains(typeName)) {
      invoke(Class.forName(typeName), typedEvent);
      return;
    }
    var eventClasses = getAllClasses("app.bpartners.api.endpoint.event.model");
    for (var clazz : eventClasses) {
      if (clazz.getTypeName().equals(typeName)) {
        invoke(clazz, typedEvent);
        return;
      }
    }

    throw new RuntimeException("Unexpected type for event=" + typedEvent);
  }

  @SneakyThrows
  private void invoke(Class<?> eventClazz, TypedEvent typedEvent) {
    var serviceClazz = Class.forName(getEventService(typedEvent.typeName()));
    var acceptMethod = serviceClazz.getMethod("accept", eventClazz);
    acceptMethod.invoke(applicationContext.getBean(serviceClazz), typedEvent.payload());
  }

  private String getEventService(String eventClazzName) {
    var typeNameAsArray = eventClazzName.split("\\.");
    return "app.bpartners.api.service.event."
        + typeNameAsArray[typeNameAsArray.length - 1]
        + "Service";
  }

  private Set<Class<?>> getAllClasses(String packageName) {
    var reflections = new Reflections(packageName, Scanners.SubTypes.filterResultsBy(s -> true));
    return new HashSet<>(reflections.getSubTypesOf(Object.class));
  }
}
