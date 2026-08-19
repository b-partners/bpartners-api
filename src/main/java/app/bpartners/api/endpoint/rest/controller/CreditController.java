package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.CreditBalanceRestMapper;
import app.bpartners.api.endpoint.rest.mapper.CreditPackRestMapper;
import app.bpartners.api.endpoint.rest.model.CreditBalance;
import app.bpartners.api.endpoint.rest.model.CreditPack;
import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.service.credit.CreditService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CreditController {
  private final CreditService service;
  private final CreditPackRestMapper creditPackRestMapper;
  private final CreditBalanceRestMapper creditBalanceRestMapper;
  private final AuthenticatedResourceProvider authenticatedResourceProvider;

  @GetMapping("/creditPacks")
  public List<CreditPack> getCreditPacks(
      @RequestParam(required = false) PageFromOne page,
      @RequestParam(required = false) BoundedPageSize pageSize) {
    var unitPrice = service.resolveCreditUnitPrice(authenticatedResourceProvider.getUser());
    return service.getCreditPacks(page, pageSize).stream()
        .map(creditPack -> creditPackRestMapper.toRest(creditPack, unitPrice))
        .toList();
  }

  @GetMapping("/creditPacks/{packId}")
  public CreditPack getCreditPackById(@PathVariable String packId) {
    var unitPrice = service.resolveCreditUnitPrice(authenticatedResourceProvider.getUser());
    return creditPackRestMapper.toRest(service.getCreditPack(packId), unitPrice);
  }

  @GetMapping("/users/{uId}/creditBalance")
  public CreditBalance getCreditBalance(@PathVariable String uId) {
    return creditBalanceRestMapper.toRest(service.getCreditBalance(uId));
  }
}
