package app.bpartners.api.repository.bridge.repository.implementation;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.repository.UserTokenRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Bank.BridgeBank;
import app.bpartners.api.repository.bridge.model.Item.BridgeCreateItem;
import app.bpartners.api.repository.bridge.model.Item.BridgeItem;
import app.bpartners.api.repository.bridge.repository.BridgeBankRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class BridgeBankRepositoryImpl implements BridgeBankRepository {
  private final BridgeApi bridgeApi;
  private final UserTokenRepository tokenRepository;

  @Override
  public BridgeBank findById(Long id) {
    return bridgeApi.findBankById(id);
  }

  @Override
  public String initiateBankConnection(String userEmail) {
    UserToken bridgeToken =
        tokenRepository.getLatestTokenByUser(AuthProvider.getPrincipal().getUser());
    return bridgeApi.initiateBankConnection(BridgeCreateItem.builder()
            .prefillEmail(userEmail)
            .build(),
        bridgeToken.getAccessToken());
  }

  @Override
  public List<BridgeItem> getBridgeItems() {
    return bridgeApi.findItemsByToken(AuthProvider.getPrincipal().getBearer());
  }
}
