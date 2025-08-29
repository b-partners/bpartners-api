package app.bpartners.api.unit.mapper;

import static app.bpartners.api.endpoint.rest.model.ContactNature.PROSPECT;
import static app.bpartners.api.endpoint.rest.model.ProspectStatus.CONTACTED;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.model.mapper.ProspectMapper;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.model.prospect.ProspectStatusHistory;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.repository.jpa.model.HProspectStatusHistory;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.GeoUtils;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.GridData;
import com.google.api.services.sheets.v4.model.Sheet;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProspectMapperTest {
  BanApi banApi = mock();
  CustomDateFormatter customDateFormatter = new CustomDateFormatter();
  ProspectMapper subject = new ProspectMapper(banApi, customDateFormatter);

  @Test
  void to_entity() {
    var lastEvaluationDate = Instant.now().minus(10, MINUTES);
    var orderedStatusHistory =
        ProspectStatusHistory.builder()
            .updatedAt(lastEvaluationDate.minus(1, MINUTES))
            .status(CONTACTED)
            .build();
    var location = new Geojson().latitude(50.10).longitude(50.10);
    var domain =
        Prospect.builder()
            .id("domain")
            .firstName("firstName")
            .phone("phone")
            .managerName("managerName")
            .name("name")
            .email("email")
            .address("address")
            .townCode(123)
            .defaultComment("defaultComment")
            .contactNature(PROSPECT)
            .statusHistories(List.of(orderedStatusHistory))
            .location(location)
            .build();
    var prospectOwnerId = "prospectOwnerId";
    var rating = 5.0;

    var actual = subject.toEntity(domain, prospectOwnerId, rating, lastEvaluationDate);

    var expected =
        HProspect.builder()
            .id(domain.getId())
            .firstName(domain.getFirstName())
            .oldPhone(domain.getPhone())
            .managerName(domain.getManagerName())
            .oldName(domain.getName())
            .oldEmail(domain.getEmail())
            .oldAddress(domain.getAddress())
            .idAccountHolder(prospectOwnerId)
            .townCode(domain.getTownCode())
            .rating(rating)
            .lastEvaluationDate(lastEvaluationDate)
            .posLongitude(location.getLongitude())
            .posLatitude(location.getLatitude())
            .statusHistories(
                List.of(
                    HProspectStatusHistory.builder()
                        .id(actual.getStatusHistories().getFirst().getId())
                        .status(domain.getActualStatus())
                        .updatedAt(actual.getStatusHistories().getFirst().getUpdatedAt())
                        .build()))
            .defaultComment(domain.getDefaultComment())
            .contactNature(domain.getContactNature())
            .build();
    assertEquals(expected, actual);
  }

  @Test
  void to_prospect_eval_ok() {
    var ownerId = "owner123";
    var sheet = new Sheet();
    var gridData = new GridData();
    var row = new com.google.api.services.sheets.v4.model.RowData();
    var cells = new java.util.ArrayList<CellData>();
    // Colonnes 0 → 13 (ProspectEvalInfo)
    cells.add(new CellData().setFormattedValue("ProspectName")); // 0 - name
    cells.add(new CellData().setFormattedValue("website")); // 1
    cells.add(new CellData().setFormattedValue("category")); // 2
    cells.add(new CellData().setFormattedValue("subcategory")); // 3
    cells.add(new CellData().setFormattedValue("Some Address")); // 4 - address
    cells.add(new CellData().setFormattedValue("0123456789")); // 5 - phone
    cells.add(new CellData().setFormattedValue("mail@test.com")); // 6 - email
    cells.add(new CellData().setFormattedValue("Manager")); // 7
    cells.add(new CellData().setFormattedValue("Yes")); // 8 - mailSent
    cells.add(new CellData().setFormattedValue("75000")); // 9 - postalCode
    cells.add(new CellData().setFormattedValue("Paris")); // 10 - city
    cells.add(new CellData().setFormattedValue("01/01/2020")); // 11 - companyCreationDate
    cells.add(new CellData().setFormattedValue("prospect")); // 12 - contactNature
    cells.add(new CellData().setFormattedValue("defaultComment")); // 13 - defaultComment
    // Colonne 14 : DEPA_RULE
    cells.add(new CellData().setFormattedValue("Dépa1 / Nouvelle intervention"));
    // Colonne 15 : jobValue (valide sinon -> NotImplementedException)
    cells.add(new CellData().setFormattedValue("Serrurier"));
    // Colonnes 16 → 20 : booléens
    cells.add(new CellData().setFormattedValue("Yes")); // 16 - insectControl
    cells.add(new CellData().setFormattedValue("No")); // 17 - disinfection
    cells.add(new CellData().setFormattedValue("Yes")); // 18 - ratRemoval
    cells.add(new CellData().setFormattedValue("Yes")); // 19 - particularCustomer
    cells.add(new CellData().setFormattedValue("No")); // 20 - professionalCustomer
    // Colonne 21 : planned
    cells.add(new CellData().setFormattedValue("Yes"));
    // Colonne 22 : interventionType (valide)
    cells.add(new CellData().setFormattedValue("Dératisation"));
    // Colonne 23 : infestationType (valide)
    cells.add(new CellData().setFormattedValue("puces"));
    // Colonne 24 : newIntAddress
    cells.add(new CellData().setFormattedValue("New Address"));
    // Colonne 25 : customerType
    cells.add(new CellData().setFormattedValue("Particulier"));
    // Colonne 26 : professionalType
    cells.add(new CellData().setFormattedValue("commerce"));
    // Colonne 27 : oldCustomerAddress
    cells.add(new CellData().setFormattedValue("Old Address"));
    while (cells.size() <= ProspectMapper.OWNER_ID_CELL_INDEX) {
      cells.add(new CellData().setFormattedValue(ownerId));
    }
    row.setValues(cells);
    gridData.setRowData(List.of(row));
    sheet.setData(List.of(gridData));
    var geoPos = new app.bpartners.api.repository.ban.model.GeoPosition();
    geoPos.setCoordinates(new GeoUtils.Coordinate(1.0, 2.0));
    org.mockito.Mockito.when(banApi.fSearch("Some Address")).thenReturn(geoPos);
    org.mockito.Mockito.when(banApi.fSearch("New Address")).thenReturn(geoPos);
    org.mockito.Mockito.when(banApi.fSearch("Old Address")).thenReturn(geoPos);

    var result = subject.toProspectEval(ownerId, sheet);

    assertEquals(1, result.size());
    var eval = result.getFirst();
    assertEquals(ownerId, eval.getProspectOwnerId());
    assertEquals("ProspectName", eval.getProspectEvalInfo().getName());
    assertEquals("Some Address", eval.getProspectEvalInfo().getAddress());
  }
}
