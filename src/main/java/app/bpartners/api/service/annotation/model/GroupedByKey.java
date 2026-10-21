package app.bpartners.api.service.annotation.model;

import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstance;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstanceInfo;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record GroupedByKey(String key, List<ExportAreaPictureAnnotationInstance> instances) {
  public static final String KEY_LABEL = "key";

  // Used by the template
  public ExportAreaPictureAnnotationInstance mergedInstance() {
    assert !this.instances.isEmpty();
    var instance = this.instances.getFirst();

    return new ExportAreaPictureAnnotationInstance()
        .labelName(key)
        .fillColor(instance.getFillColor())
        .infos(infos())
        .measurements(instance.getMeasurements())
        .polygon(instance.getPolygon());
  }

  public List<ExportAreaPictureAnnotationInstanceInfo> infos() {
    return this.instances.stream()
        .map(ExportAreaPictureAnnotationInstance::getInfos)
        .map(infos -> infos.stream().filter(info -> !KEY_LABEL.equals(info.getLabel())).toList())
        .max(Comparator.comparing(List::size))
        .orElse(List.of());
  }

  public static List<GroupedByKey> from(List<ExportAreaPictureAnnotationInstance> instances) {
    Map<String, List<ExportAreaPictureAnnotationInstance>> grouped = new LinkedHashMap<>();

    for (var instance : instances) {
      String key = getKey(instance);
      grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(instance);
    }

    List<GroupedByKey> result = new ArrayList<>();
    for (var entry : grouped.entrySet()) {
      result.add(new GroupedByKey(entry.getKey(), entry.getValue()));
    }

    return result;
  }

  public static String getKey(ExportAreaPictureAnnotationInstance instance) {
    return instance.getInfos().stream()
        .filter(info -> KEY_LABEL.equals(info.getLabel()))
        .map(ExportAreaPictureAnnotationInstanceInfo::getValue)
        .findFirst()
        .orElse("generated_key_" + randomUUID());
  }
}
