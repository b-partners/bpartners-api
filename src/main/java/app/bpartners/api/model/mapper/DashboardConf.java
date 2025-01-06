package app.bpartners.api.model.mapper;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class DashboardConf {
  private final String dashboardUrl;

  public DashboardConf(@Value("DASHBOARD_URL") String dashUrl) {
    dashboardUrl = dashUrl;
  }
}
