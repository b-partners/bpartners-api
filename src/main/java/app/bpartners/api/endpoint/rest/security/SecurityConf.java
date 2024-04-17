package app.bpartners.api.endpoint.rest.security;

import static app.bpartners.api.endpoint.rest.security.model.Role.EVAL_PROSPECT;
import static app.bpartners.api.endpoint.rest.security.model.Role.INVOICE_RELAUNCHER;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

import app.bpartners.api.endpoint.rest.security.matcher.SelfAccountHolderMatcher;
import app.bpartners.api.endpoint.rest.security.matcher.SelfAccountMatcher;
import app.bpartners.api.endpoint.rest.security.matcher.SelfUserAccountMatcher;
import app.bpartners.api.endpoint.rest.security.matcher.SelfUserMatcher;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.service.LegalFileService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@Slf4j
@EnableWebSecurity
public class SecurityConf {

  public static final String AUTHORIZATION_HEADER = "Authorization";
  private final AuthProvider authProvider;
  private final HandlerExceptionResolver exceptionResolver;
  private final AuthenticatedResourceProvider authResourceProvider;
  private final LegalFileService legalFileService;

  public SecurityConf(
      AuthProvider authProvider,
      // InternalToExternalErrorHandler behind
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver,
      AuthenticatedResourceProvider authResourceProvider,
      LegalFileService legalFileService) {
    this.authProvider = authProvider;
    this.exceptionResolver = exceptionResolver;
    this.authResourceProvider = authResourceProvider;
    this.legalFileService = legalFileService;
  }

  @Bean
  public SecurityFilterChain configure(HttpSecurity http) throws Exception {
    // @formatter:off
    http.exceptionHandling(
            (exceptionHandler) ->
                exceptionHandler
                    .authenticationEntryPoint(
                        // note(spring-exception)
                        // https://stackoverflow.com/questions/59417122/how-to-handle-usernamenotfoundexception-spring-security
                        // issues like when a user tries to access a resource
                        // without appropriate authentication elements
                        (req, res, e) ->
                            exceptionResolver.resolveException(
                                req, res, null, forbiddenWithRemoteInfo(e, req)))
                    .accessDeniedHandler(
                        // note(spring-exception): issues like when a user not having required roles
                        (req, res, e) ->
                            exceptionResolver.resolveException(
                                req, res, null, forbiddenWithRemoteInfo(e, req))))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // authenticate
        .authenticationProvider(authProvider)
        .addFilterBefore(
            bearerFilter(new AntPathRequestMatcher("/**")), AnonymousAuthenticationFilter.class)
        .addFilterAfter(
            legalFilesApprovalFilter(
                new OrRequestMatcher(
                    new AntPathRequestMatcher("/accountHolders", GET.name()),
                    new AntPathRequestMatcher("/invoicesRefresh", POST.name()),
                    new AntPathRequestMatcher("/accounts/*/customers", GET.name()),
                    new AntPathRequestMatcher("/users/accounts/refresh", POST.name()),
                    new AntPathRequestMatcher("/accounts/*/customers/export", GET.name()),
                    new AntPathRequestMatcher("/accounts/*/customers", POST.name()),
                    new AntPathRequestMatcher("/accounts/*/customers", PUT.name()),
                    new AntPathRequestMatcher("/accounts/*/customers/*", GET.name()),
                    new AntPathRequestMatcher("/accounts/*/customers/upload", POST.name()),
                    new AntPathRequestMatcher("/accounts/{id}/customers/status", PUT.name()),
                    new AntPathRequestMatcher("/accounts/*/transactions", GET.name()),
                    new AntPathRequestMatcher("/accounts/*/transactions/*", GET.name()),
                    new AntPathRequestMatcher("/accounts/*/transactions/exportLink", POST.name()),
                    new AntPathRequestMatcher("/accounts/*/transactions/*/invoices/*", PUT.name()),
                    new AntPathRequestMatcher(
                        "/accounts/*/transactions/*/supportingDocuments", GET.name()),
                    new AntPathRequestMatcher(
                        "/accounts/*/transactions/*/supportingDocuments", POST.name()),
                    new AntPathRequestMatcher(
                        "/accounts/*/transactions/*/supportingDocuments", DELETE.name()),
                    new AntPathRequestMatcher("/users/*/accounts", GET.name()),
                    new AntPathRequestMatcher("/users/*/deviceRegistration", POST.name()),
                    new AntPathRequestMatcher("/users/*/accounts/*/accountHolders", GET.name()),
                    new AntPathRequestMatcher(
                        "/accountHolders/*/prospects/evaluationJobs/*", GET.name()),
                    new AntPathRequestMatcher(
                        "/users/*/accounts/*/initiateAccountValidation", POST.name()),
                    new AntPathRequestMatcher("/users/*/accounts/*/identity", PUT.name()),
                    new AntPathRequestMatcher("/users/*/accounts/*/active", POST.name()),
                    new AntPathRequestMatcher(
                        "/users/*/accounts/*/initiateBankConnection", POST.name()),
                    new AntPathRequestMatcher("/users/*/initiateBankConnection", POST.name()),
                    new AntPathRequestMatcher("/users/*/disconnectBank", POST.name()),
                    new AntPathRequestMatcher("/accounts/*/invoices/*", GET.name()),
                    new AntPathRequestMatcher("/accounts/*/invoices/*", PUT.name()),
                    new AntPathRequestMatcher("/accounts/*/invoices/archive", PUT.name()),
                    new AntPathRequestMatcher("/accounts/*/products", GET.name()),
                    new AntPathRequestMatcher("/accounts/*/products/export", GET.name()),
                    new AntPathRequestMatcher("/accounts/*/products/*", GET.name()),
                    new AntPathRequestMatcher("/accounts/*/products", PUT.name()),
                    new AntPathRequestMatcher("/accounts/*/products", POST.name()),
                    new AntPathRequestMatcher("/accounts/*/products/upload", POST.name()),
                    new AntPathRequestMatcher("/accounts/*/products/status", PUT.name()),
                    new AntPathRequestMatcher("/accounts/*/invoices", GET.name()),
                    new AntPathRequestMatcher("/accounts/*/paymentInitiations", POST.name()),
                    new AntPathRequestMatcher("/accounts/*/transactionCategories", GET.name()),
                    new AntPathRequestMatcher(
                        "/accounts/*/transactions/*/transactionCategories", GET.name()),
                    new AntPathRequestMatcher("/users", GET.name()),
                    new AntPathRequestMatcher("/accounts/*/files/*", GET.name()),
                    new AntPathRequestMatcher("/accounts/*/areaPictures", GET.name()),
                    new AntPathRequestMatcher("/accounts/*/areaPictures/*", GET.name()),
                    new AntPathRequestMatcher("/accounts/*/areaPictures/*/raw", PUT.name()),
                    new AntPathRequestMatcher("/accounts/*/files/*/raw", POST.name()),
                    new AntPathRequestMatcher("/accounts/*/files/*/multipart", POST.name()),
                    new AntPathRequestMatcher("/accounts/*/files/*/raw", GET.name()),
                    new AntPathRequestMatcher("/accounts/*/marketplaces", GET.name()),
                    new AntPathRequestMatcher("/accounts/*/transactionsSummary", GET.name()),
                    new AntPathRequestMatcher("/accounts/*/invoicesSummary", GET.name()),
                    new AntPathRequestMatcher("/accounts/*/invoiceRelaunchConf", GET.name()),
                    new AntPathRequestMatcher("/accounts/*/invoiceRelaunchConf", PUT.name()),
                    new AntPathRequestMatcher("/accounts/*/invoices/*/relaunches", GET.name()),
                    new AntPathRequestMatcher("/accounts/*/invoices/*/relaunch", POST.name()),
                    new AntPathRequestMatcher("/accounts/*/invoices/relaunches", POST.name()),
                    new AntPathRequestMatcher("/accounts/*/invoices/*/relaunchConf", GET.name()),
                    new AntPathRequestMatcher("/accounts/*/invoices/*/relaunchConf", PUT.name()),
                    new AntPathRequestMatcher("/accounts/*/invoices/*/duplication", POST.name()),
                    new AntPathRequestMatcher("/businessActivities", GET.name()),
                    new AntPathRequestMatcher(
                        "/users/*/accounts/*/accountHolders/*/businessActivities", PUT.name()),
                    new AntPathRequestMatcher(
                        "/users/*/accounts/*/accountHolders/*/revenueTargets", PUT.name()),
                    new AntPathRequestMatcher(
                        "/users/*/accounts/*/accountHolders/*/companyInfo", PUT.name()),
                    new AntPathRequestMatcher(
                        "/users/*/accounts/*/accountHolders/*/globalInfo", PUT.name()),
                    new AntPathRequestMatcher("/users/*/accountHolders/*/feedback/*", PUT.name()),
                    new AntPathRequestMatcher("/users/*/accountHolders/*/feedback", POST.name()),
                    new AntPathRequestMatcher("/accounts/*/paymentRequests", GET.name()),
                    new AntPathRequestMatcher("/accountHolders/*/prospects", GET.name()),
                    new AntPathRequestMatcher(
                        "/accountHolders/*/prospects/evaluationJobs", GET.name()),
                    new AntPathRequestMatcher(
                        "/accountHolders/*/prospects/evaluationJobs", PUT.name()),
                    new AntPathRequestMatcher(
                        "/accountHolders/*/prospects/*/prospectConversion", PUT.name()),
                    new AntPathRequestMatcher("/accountHolders/*/prospects", PUT.name()),
                    new AntPathRequestMatcher(
                        "/accountHolders/*/prospects/prospectsEvaluation", POST.name()),
                    new AntPathRequestMatcher(
                        "/accountHolders/*/prospects/evaluations", POST.name()),
                    new AntPathRequestMatcher("/accountHolders/*/prospects/import", POST.name()),
                    new AntPathRequestMatcher("/accountHolders/*/prospects/*", PUT.name()),
                    new AntPathRequestMatcher("/users/*/calendars/*/events", GET.name()),
                    new AntPathRequestMatcher("/users/*/calendars/*/events", PUT.name()),
                    new AntPathRequestMatcher("/users/*/calendars/oauth2/consent", POST.name()),
                    new AntPathRequestMatcher("/users/*/calendars", GET.name()),
                    new AntPathRequestMatcher("/users/*/calendars/oauth2/auth", POST.name()),
                    new AntPathRequestMatcher("/users/*/sheets/oauth2/consent", POST.name()),
                    new AntPathRequestMatcher("/users/*/sheets/oauth2/auth", POST.name()),
                    new AntPathRequestMatcher(
                        "/accounts/*/invoices/{iId}/paymentRegulations/*/paymentMethod",
                        PUT.name()),
                    new AntPathRequestMatcher("/users/*/emails", PUT.name()),
                    new AntPathRequestMatcher("/users/*/emails", GET.name()))),
            BearerAuthFilter.class)
        // authorize
        .authorizeHttpRequests(
            (authorize) ->
                authorize
                    .requestMatchers("/ping")
                    .permitAll()
                    .requestMatchers("/authInitiation")
                    .permitAll()
                    .requestMatchers("/token")
                    .permitAll()
                    .requestMatchers(GET, "/whoami")
                    .permitAll()
                    .requestMatchers("/onboarding")
                    .permitAll()
                    .requestMatchers(GET, "/users/*")
                    .permitAll()
                    .requestMatchers("/onboardingInitiation")
                    .permitAll()
                    .requestMatchers(POST, "/preUsers")
                    .permitAll()
                    .requestMatchers(
                        new SelfUserMatcher(GET, "/users/*/legalFiles", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfUserMatcher(PUT, "/users/*/legalFiles/*", authResourceProvider))
                    .authenticated()
                    .requestMatchers(OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(GET, "/whois/*")
                    .permitAll()
                    .requestMatchers(POST, "/webhooks/paymentStatus")
                    .permitAll()
                    .requestMatchers(GET, "/health/db")
                    .permitAll()
                    .requestMatchers(GET, "/health/bucket")
                    .permitAll()
                    .requestMatchers(GET, "/health/event")
                    .permitAll()
                    .requestMatchers(GET, "/health/email")
                    .permitAll()
                    .requestMatchers(GET, "/accountHolders")
                    .hasAnyRole(EVAL_PROSPECT.getRole())
                    .requestMatchers(POST, "/invoicesRefresh")
                    .hasAnyRole(EVAL_PROSPECT.getRole())
                    .requestMatchers(
                        new SelfAccountMatcher(GET, "/accounts/*/customers", authResourceProvider))
                    .authenticated()
                    .requestMatchers(POST, "/invoicesRefresh")
                    .hasAnyRole(EVAL_PROSPECT.getRole())
                    .requestMatchers(POST, "/users/accounts/refresh")
                    .hasAnyRole(EVAL_PROSPECT.getRole())
                    .requestMatchers(
                        new SelfAccountMatcher(
                            GET, "/accounts/*/customers/export", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(POST, "/accounts/*/customers", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(PUT, "/accounts/*/customers", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            GET, "/accounts/*/customers/*", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            POST, "/accounts/*/customers/upload", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            PUT, "/accounts/{id}/customers/status", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            GET, "/accounts/*/transactions", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            GET, "/accounts/*/transactions/*", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            POST, "/accounts/*/transactions/exportLink", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            PUT, "/accounts/*/transactions/*/invoices/*", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            GET, "/accounts/*/transactions/*", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            POST, "/accounts/*/transactions/exportLink", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            PUT, "/accounts/*/transactions/*/invoices/*", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            GET,
                            "/accounts/*/transactions/*/supportingDocuments",
                            authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            POST,
                            "/accounts/*/transactions/*/supportingDocuments",
                            authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            DELETE,
                            "/accounts/*/transactions/*/supportingDocuments",
                            authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfUserMatcher(GET, "/users/*/accounts", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfUserMatcher(
                            POST, "/users/*/deviceRegistration", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfUserAccountMatcher(
                            GET, "/users/*/accounts/*/accountHolders", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountHolderMatcher(
                            GET,
                            "/accountHolders/*/prospects/evaluationJobs/*",
                            authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfUserAccountMatcher(
                            POST,
                            "/users/*/accounts/*/initiateAccountValidation",
                            authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfUserAccountMatcher(
                            PUT, "/users/*/accounts/*/identity", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfUserAccountMatcher(
                            POST, "/users/*/accounts/*/active", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfUserAccountMatcher(
                            POST,
                            "/users/*/accounts/*/initiateBankConnection",
                            authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfUserMatcher(
                            POST, "/users/*/initiateBankConnection", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfUserMatcher(POST, "/users/*/disconnectBank", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(GET, "/accounts/*/invoices/*", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(PUT, "/accounts/*/invoices/*", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            PUT, "/accounts/*/invoices/archive", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(GET, "/accounts/*/products", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            GET, "/accounts/*/products/export", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(GET, "/accounts/*/products/*", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(PUT, "/accounts/*/products", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(POST, "/accounts/*/products", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            POST, "/accounts/*/products/upload", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            PUT, "/accounts/{aId}/products/status", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(GET, "/accounts/*/invoices", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            POST, "/accounts/*/paymentInitiations", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            GET, "/accounts/*/transactionCategories", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            POST,
                            "/accounts/*/transactions/*/transactionCategories",
                            authResourceProvider))
                    .authenticated()
                    .requestMatchers(GET, "/users")
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(GET, "/accounts/*/files/*", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            GET, "/accounts/*/areaPictures", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            GET, "/accounts/*/areaPictures/*", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            PUT, "/accounts/*/areaPictures/*/raw", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            POST, "/accounts/*/files/*/raw", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            POST, "/accounts/*/files/*/multipart", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            GET, "/accounts/*/files/*/raw", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            GET, "/accounts/*/marketplaces", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            GET, "/accounts/*/transactionsSummary", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            GET, "/accounts/*/invoicesSummary", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            GET, "/accounts/*/invoiceRelaunchConf", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            PUT, "/accounts/*/invoiceRelaunchConf", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            GET, "/accounts/*/invoices/*/relaunches", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            POST, "/accounts/*/invoices/*/relaunch", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            POST, "/accounts/*/invoices/relaunches", authResourceProvider))
                    .hasAnyRole(INVOICE_RELAUNCHER.getRole())
                    .requestMatchers(
                        new SelfAccountMatcher(
                            GET, "/accounts/*/invoices/*/relaunchConf", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            PUT, "/accounts/*/invoices/*/relaunchConf", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            POST, "/accounts/*/invoices/*/duplication", authResourceProvider))
                    .authenticated()
                    .requestMatchers(GET, "/businessActivities")
                    .authenticated()
                    // TODO: set SelfUserAccountHolderMatcher
                    .requestMatchers(PUT, "/users/*/accounts/*/accountHolders/*/businessActivities")
                    .authenticated()
                    .requestMatchers(PUT, "/users/*/accounts/*/accountHolders/*/revenueTargets")
                    .authenticated()
                    .requestMatchers(PUT, "/users/*/accounts/*/accountHolders/*/companyInfo")
                    .authenticated()
                    .requestMatchers(PUT, "/users/*/accounts/*/accountHolders/*/globalInfo")
                    .authenticated()
                    .requestMatchers(PUT, "/users/*/accountHolders/*/feedback/*")
                    .authenticated()
                    .requestMatchers(POST, "/users/*/accountHolders/*/feedback")
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            GET, "/accounts/*/paymentRequests", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountHolderMatcher(
                            GET, "/accountHolders/*/prospects", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountHolderMatcher(
                            GET,
                            "/accountHolders/*/prospects/evaluationJobs",
                            authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountHolderMatcher(
                            PUT,
                            "/accountHolders/*/prospects/evaluationJobs",
                            authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountHolderMatcher(
                            PUT,
                            "/accountHolders/*/prospects/*/prospectConversion",
                            authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountHolderMatcher(
                            PUT, "/accountHolders/*/prospects", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountHolderMatcher(
                            POST,
                            "/accountHolders/*/prospects/prospectsEvaluation",
                            authResourceProvider))
                    .hasAnyRole(EVAL_PROSPECT.getRole())
                    .requestMatchers(
                        new SelfAccountHolderMatcher(
                            POST, "/accountHolders/*/prospects/evaluations", authResourceProvider))
                    .hasAnyRole(EVAL_PROSPECT.getRole())
                    .requestMatchers(
                        new SelfAccountHolderMatcher(
                            POST, "/accountHolders/*/prospects/import", authResourceProvider))
                    .hasAnyRole(EVAL_PROSPECT.getRole())
                    .requestMatchers(
                        new SelfAccountHolderMatcher(
                            PUT, "/accountHolders/*/prospects/*", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfUserMatcher(
                            GET, "/users/*/calendars/*/events", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfUserMatcher(
                            PUT, "/users/*/calendars/*/events", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfUserMatcher(
                            POST, "/users/*/calendars/oauth2/consent", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfUserMatcher(GET, "/users/*/calendars", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfUserMatcher(
                            POST, "/users/*/calendars/oauth2/auth", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfUserMatcher(
                            POST, "/users/*/sheets/oauth2/consent", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfUserMatcher(
                            POST, "/users/*/sheets/oauth2/auth", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfAccountMatcher(
                            PUT,
                            "/accounts/*/invoices/{iId}/paymentRegulations/*/paymentMethod",
                            authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfUserMatcher(PUT, "/users/*/emails", authResourceProvider))
                    .authenticated()
                    .requestMatchers(
                        new SelfUserMatcher(GET, "/users/*/emails", authResourceProvider))
                    .authenticated()
                    .requestMatchers("/**")
                    .denyAll())

        // disable superfluous protections
        // Eg if all clients are non-browser then no csrf
        // https://docs.spring.io/spring-security/site/docs/3.2.0.CI-SNAPSHOT/reference/html/csrf.html,
        // Sec 13.3
        .csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable);
    // formatter:on
    return http.build();
  }

  private Exception forbiddenWithRemoteInfo(Exception e, HttpServletRequest req) {
    log.info(
        String.format(
            "Access is denied for remote caller: address=%s, host=%s, port=%s",
            req.getRemoteAddr(), req.getRemoteHost(), req.getRemotePort()));
    return new ForbiddenException(e.getMessage());
  }

  @Bean
  public AuthenticationManager authenticationManager() {
    return new ProviderManager(authProvider);
  }

  private BearerAuthFilter bearerFilter(RequestMatcher requiresAuthenticationRequestMatcher) {
    BearerAuthFilter bearerFilter =
        new BearerAuthFilter(requiresAuthenticationRequestMatcher, AUTHORIZATION_HEADER);
    bearerFilter.setAuthenticationManager(authenticationManager());
    bearerFilter.setAuthenticationSuccessHandler(
        (httpServletRequest, httpServletResponse, authentication) -> {});
    bearerFilter.setAuthenticationFailureHandler(
        (req, res, e) ->
            // note(spring-exception)
            // issues like when a user is not found(i.e. UsernameNotFoundException)
            // or other exceptions thrown inside authentication provider.
            // In fact, this handles other authentication exceptions that are
            // not handled by AccessDeniedException and AuthenticationEntryPoint
            exceptionResolver.resolveException(req, res, null, forbiddenWithRemoteInfo(e, req)));
    return bearerFilter;
  }

  private LegalFilesApprovalFilter legalFilesApprovalFilter(
      RequestMatcher requiresLegalFileApprovalRequestMatcher) {
    return new LegalFilesApprovalFilter(
        requiresLegalFileApprovalRequestMatcher, legalFileService, authResourceProvider);
  }
}
