package app.bpartners.api.integration.event;

import app.bpartners.api.endpoint.event.model.MonthlyCancelledClientsPayment;
import app.bpartners.api.integration.conf.StripeMockedThirdParties;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.UserSubscriptionSession;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionSessionRepository;
import app.bpartners.api.service.event.MonthlyCancelledClientsPaymentService;
import app.bpartners.api.service.subscription.StripeSessionFactory;
import app.bpartners.api.service.utils.TemporalUtils;
import com.stripe.StripeClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MonthlyCancelledClientsPaymentServiceIT extends StripeMockedThirdParties {
    UserSubscriptionSessionRepository userSubscriptionSessionRepositoryMock = mock();
    TemporalUtils temporalUtilsMock = mock();
    UserRepository userRepositoryMock = mock();
    SubscriptionProductRepository productRepositoryMock = mock();
    @Autowired StripeClient stripeClient ;
    MonthlyCancelledClientsPaymentService subject = new MonthlyCancelledClientsPaymentService(
            userSubscriptionSessionRepositoryMock,
            temporalUtilsMock,
            userRepositoryMock,
            productRepositoryMock,
            stripeClient);

    @Test
    void generate_punctual_invoice(){
        var userSubscriptionSession = UserSubscriptionSession.builder()
                .userId("user_id")
                .setUpUntil(LocalDate.now().plusDays(1)).build();
        when(userSubscriptionSessionRepositoryMock.findAll()).thenReturn(List.of(userSubscriptionSession));
        var user = User.builder().build();
        when(userRepositoryMock.getById(any())).thenReturn(user);
        var subscriptionProduct = SubscriptionProduct.builder().build();
        when(productRepositoryMock.findByPriceInCents(anyDouble())).thenReturn(subscriptionProduct);

        subject.accept(MonthlyCancelledClientsPayment.builder().build());
    }
}
