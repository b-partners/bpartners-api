package app.bpartners.api.repository.jpa.model.converter;

import app.bpartners.api.service.WMS.MapLayer;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OpenStreetMapLayerConverter implements AttributeConverter<MapLayer, String> {

  @Override
  public String convertToDatabaseColumn(MapLayer attribute) {
    return attribute.getValue();
  }

  @Override
  public MapLayer convertToEntityAttribute(String dbData) {
    return MapLayer.fromValue(dbData);
  }
}
