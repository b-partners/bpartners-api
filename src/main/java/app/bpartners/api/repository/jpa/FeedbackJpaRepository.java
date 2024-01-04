package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackJpaRepository extends JpaRepository<HFeedback, String> {}
