package app.bpartners.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Marketplace {
  private String id;
  private String name;
  private String description;
  private String phoneNumber;
  private String logoUrl;
  private String websiteUrl;
}
