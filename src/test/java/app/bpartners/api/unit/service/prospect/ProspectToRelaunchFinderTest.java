package app.bpartners.api.unit.service.prospect;

import static app.bpartners.api.endpoint.rest.model.ProspectStatus.TO_CONTACT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.service.prospect.ProspectStatusService;
import app.bpartners.api.service.prospect.relaunch.ProspectToRelaunchFinder;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProspectToRelaunchFinderTest {
  private static final String HOLDER_ID = "holder_id";
  private static final String OTHER_HOLDER_ID = "other_holder_id";
  ProspectStatusService statusServiceMock = mock(ProspectStatusService.class);
  ProspectToRelaunchFinder subject = new ProspectToRelaunchFinder(statusServiceMock);

  @Test
  void groups_rated_prospects_by_holder() {
    var firstProspect = ratedProspect("prospect_1", HOLDER_ID, 8.0);
    var secondProspect = ratedProspect("prospect_2", HOLDER_ID, 5.0);
    var otherHolderProspect = ratedProspect("prospect_3", OTHER_HOLDER_ID, 9.0);
    when(statusServiceMock.findAllByStatus(TO_CONTACT))
        .thenReturn(List.of(firstProspect, secondProspect, otherHolderProspect));

    var actual = subject.findGroupedByHolder();

    assertEquals(2, actual.size());
    assertEquals(List.of(firstProspect, secondProspect), actual.get(HOLDER_ID));
    assertEquals(List.of(otherHolderProspect), actual.get(OTHER_HOLDER_ID));
  }

  @Test
  void ignores_prospects_without_positive_rating() {
    when(statusServiceMock.findAllByStatus(TO_CONTACT))
        .thenReturn(
            List.of(
                prospect("no_rating", HOLDER_ID, null),
                ratedProspect("null_rating_value", HOLDER_ID, null),
                ratedProspect("zero_rating", HOLDER_ID, 0.0),
                ratedProspect("negative_rating", HOLDER_ID, -1.0)));

    var actual = subject.findGroupedByHolder();

    assertTrue(actual.isEmpty());
  }

  @Test
  void ignores_given_up_prospects() {
    var ownedProspect = ratedProspect("owned", HOLDER_ID, 8.0);
    when(statusServiceMock.findAllByStatus(TO_CONTACT))
        .thenReturn(List.of(ownedProspect, ratedProspect("given_up", null, 8.0)));

    var actual = subject.findGroupedByHolder();

    assertEquals(1, actual.size());
    assertEquals(List.of(ownedProspect), actual.get(HOLDER_ID));
  }

  private static Prospect ratedProspect(String id, String idHolderOwner, Double ratingValue) {
    return prospect(
        id, idHolderOwner, Prospect.ProspectRating.builder().value(ratingValue).build());
  }

  private static Prospect prospect(
      String id, String idHolderOwner, Prospect.ProspectRating rating) {
    return Prospect.builder().id(id).idHolderOwner(idHolderOwner).rating(rating).build();
  }
}
