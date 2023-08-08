package app.bpartners.api.model;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@Builder
@EqualsAndHashCode
public class Location implements Serializable {
  private String address;
  private Double latitude;
  private Double longitude;

}
