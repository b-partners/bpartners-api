package app.bpartners.api.model.subscription;

import app.bpartners.api.model.UserSubscriptionTrial;

public record StartedSubscriptionTrial(
    UserSubscriptionTrial trial, long grantedAnalyses, long grantedCredits) {}
