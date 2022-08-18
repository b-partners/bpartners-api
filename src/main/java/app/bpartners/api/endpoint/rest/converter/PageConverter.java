package app.bpartners.api.endpoint.rest.converter;

import org.springframework.core.convert.converter.Converter;
import app.bpartners.api.model.entity.PageFromOne;

public class PageConverter implements Converter<String, PageFromOne> {

  @Override
  public PageFromOne convert(String source) {
    return new PageFromOne(Integer.parseInt(source));
  }
}
