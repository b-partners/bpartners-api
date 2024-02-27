package app.bpartners.api.repository.jpa.model;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "\"business_activity\"")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HBusinessActivity {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  @ManyToOne
  @JoinColumn(name = "primary_activity", referencedColumnName = "name")
  private HBusinessActivityTemplate primaryActivity;

  @ManyToOne
  @JoinColumn(name = "secondary_activity", referencedColumnName = "name")
  private HBusinessActivityTemplate secondaryActivity;

  private String otherPrimaryActivity;
  private String otherSecondaryActivity;

  @ManyToOne
  @JoinColumn(name = "account_holder_id")
  private HAccountHolder accountHolder;
}
