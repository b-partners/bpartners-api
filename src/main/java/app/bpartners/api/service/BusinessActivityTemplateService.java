package app.bpartners.api.service;

import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.BusinessActivityTemplate;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.repository.BusinessActivityTemplateRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class BusinessActivityTemplateService {
  private final BusinessActivityTemplateRepository repository;

  public List<BusinessActivityTemplate> getBusinessActivities(PageFromOne page,
                                                              BoundedPageSize pageSize) {
    int pageValue = page != null ? page.getValue() - 1 : 0;
    int pageSizeValue = pageSize != null ? pageSize.getValue() : 30;
    Pageable pageable = PageRequest.of(pageValue, pageSizeValue);
    return repository.findAll(pageable);
  }
}
