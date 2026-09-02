package app.bpartners.api.service.subscription;

import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionProduct;

public record ResolvedPlan(SubscriptionProduct product, BillingInterval billingInterval) {}
