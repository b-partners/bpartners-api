package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HMunicipality;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MunicipalityJpaRepository extends JpaRepository<HMunicipality, String> {

  /**
   * The source SRID (Spatial Reference ID) used for the coordinates in the database. EPSG 4326 or
   * WGS84 is the SRID that GeoJson uses source : <a href
   * ="https://www.rfc-editor.org/rfc/rfc7946#section-4">RFC7946-Section-4 CRS</a>
   */
  String sourceSRID = "4326";

  @Query(
      value =
          "WITH prospecting_municipality AS ("
              + "  SELECT id, coordinates FROM municipality WHERE code = :code"
              + ") "
              + "SELECT m.id, m.name, m.code "
              + "FROM municipality m ,prospecting_municipality pm "
              + "WHERE ST_DistanceSphere("
              + "ST_Centroid(ST_SetSRID(m.coordinates, "
              + sourceSRID
              + ")), "
              + "ST_Centroid(ST_SetSRID(pm.coordinates, "
              + sourceSRID
              + "))"
              + ") <= :distance * 1000",
      nativeQuery = true)
  List<HMunicipality> findMunicipalitiesWithinDistance(
      @Param("code") String prospectingMunicipalityCode, @Param("distance") int distanceInKms);
}
