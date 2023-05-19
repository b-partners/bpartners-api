package app.bpartners.api.repository;

import app.bpartners.api.model.Feedback;
import java.util.List;

public interface FeedBackRepository {
  Feedback findById(String id);

  Feedback save(Feedback toCreate);
}
