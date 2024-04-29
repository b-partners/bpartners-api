package app.bpartners.api;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.crac.Core;
import org.crac.Resource;

@PojaGenerated
public class ApiEventHandler implements RequestStreamHandler, Resource {
  private static final SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
  private final DataSource dataSource;
  private static final Logger logger = Logger.getLogger(ApiEventHandler.class.getName());

  public ApiEventHandler(DataSource dataSource) {
    Core.getGlobalContext().register(this);
    this.dataSource = dataSource;
  }

  static {
    try {
      handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(PojaApplication.class);
    } catch (ContainerInitializationException e) {
      throw new RuntimeException("Initialization of Spring Boot Application failed", e);
    }
  }

  @Override
  public void handleRequest(InputStream input, OutputStream output, Context context)
      throws IOException {
    handler.proxyStream(input, output, context);
  }

  @Override
  public void beforeCheckpoint(org.crac.Context<? extends Resource> context) throws Exception {
    refreshDatabaseConnection();
  }

  @Override
  public void afterRestore(org.crac.Context<? extends Resource> context) throws Exception {
    refreshDatabaseConnection();
  }

  private void refreshDatabaseConnection() {
    try {
      Connection connection = dataSource.getConnection();
      connection.close();
      logger.info("Database connection refreshed successfully.");
    } catch (SQLException e) {
      logger.log(Level.SEVERE, "Error refreshing database connection", e);
    }
  }
}
