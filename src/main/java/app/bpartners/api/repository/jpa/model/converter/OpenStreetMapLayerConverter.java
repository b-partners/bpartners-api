package app.bpartners.api.repository.jpa.model.converter;

import app.bpartners.api.endpoint.rest.model.OpenStreetMapLayer;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OpenStreetMapLayerConverter implements AttributeConverter<OpenStreetMapLayer, String> {

  @Override
  public String convertToDatabaseColumn(OpenStreetMapLayer attribute) {
    return attribute.getValue();
  }

  @Override
  public OpenStreetMapLayer convertToEntityAttribute(String dbData) {
    return OpenStreetMapLayer.fromValue(dbData);
  }
}
