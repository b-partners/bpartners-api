package app.bpartners.api.service.annotation.model;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstance;
import app.bpartners.api.endpoint.rest.model.ExportAreaPictureAnnotationInstanceInfo;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

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
    return instances.stream()
        .collect(groupingBy(GroupedByKey::getKey, LinkedHashMap::new, toList()))
        .entrySet()
        .stream()
        .map(entry -> new GroupedByKey(entry.getKey(), entry.getValue()))
        .toList();
  }

  public static String getKey(ExportAreaPictureAnnotationInstance instance) {
    return instance.getInfos().stream()
        .filter(info -> KEY_LABEL.equals(info.getLabel()))
        .map(ExportAreaPictureAnnotationInstanceInfo::getValue)
        .findFirst()
        .orElse("generated_key_" + randomUUID());
  }
}
