package app.bpartners.api.endpoint.rest.security;

import app.bpartners.api.endpoint.rest.security.matcher.SelfAccountHolderMatcher;
import app.bpartners.api.endpoint.rest.security.matcher.SelfAccountMatcher;
import app.bpartners.api.endpoint.rest.security.matcher.SelfUserAccountMatcher;
import app.bpartners.api.endpoint.rest.security.matcher.SelfUserMatcher;
import app.bpartners.api.model.exception.ForbiddenException;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;

import static app.bpartners.api.endpoint.rest.security.model.Role.EVAL_PROSPECT;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

@Configuration
@Slf4j
public class SecurityConf extends WebSecurityConfigurerAdapter {

  public static final String AUTHORIZATION_HEADER = "Authorization";
  private final AuthProvider authProvider;
  private final HandlerExceptionResolver exceptionResolver;
  private final AuthenticatedResourceProvider authResourceProvider;


  public SecurityConf(
      AuthProvider authProvider,
      // InternalToExternalErrorHandler behind
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver,
      AuthenticatedResourceProvider authResourceProvider) {
    this.authProvider = authProvider;
    this.exceptionResolver = exceptionResolver;
    this.authResourceProvider = authResourceProvider;
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // @formatter:off
    http
        .exceptionHandling()
        .authenticationEntryPoint(
            // note(spring-exception)
            // https://stackoverflow.com/questions/59417122/how-to-handle-usernamenotfoundexception-spring-security
            // issues like when a user tries to access a resource
            // without appropriate authentication elements
            (req, res, e) -> exceptionResolver
                .resolveException(req, res, null, forbiddenWithRemoteInfo(e, req)))
        .accessDeniedHandler(
            // note(spring-exception): issues like when a user not having required roles
            (req, res, e) -> exceptionResolver
                .resolveException(req, res, null, forbiddenWithRemoteInfo(e, req)))

        // authenticate
        .and()
        .authenticationProvider(authProvider)
        .addFilterBefore(
            bearerFilter(new NegatedRequestMatcher(
                new OrRequestMatcher(
                    new AntPathRequestMatcher("/ping"),
                    new AntPathRequestMatcher("/preUsers", POST.name()),
                    new AntPathRequestMatcher("/authInitiation"),
                    new AntPathRequestMatcher("/token"),
                    new AntPathRequestMatcher("/onboarding"),
                    new AntPathRequestMatcher("/whoami", GET.name()),
                    new AntPathRequestMatcher("/users/*", GET.name()),
                    new AntPathRequestMatcher("/onboardingInitiation", POST.name()),
                    new AntPathRequestMatcher("/users/*/legalFiles", GET.name()),
                    new AntPathRequestMatcher("/users/*/legalFiles/*", PUT.name()),
                    new AntPathRequestMatcher("/**", OPTIONS.toString()),
                    new AntPathRequestMatcher("/whois/*", GET.name())
                )
            )),
            AnonymousAuthenticationFilter.class)
        .anonymous()

        // authorize
        .and()
        .authorizeRequests()
        .antMatchers("/ping").permitAll()
        .antMatchers("/authInitiation").permitAll()
        .antMatchers("/token").permitAll()
        .antMatchers(GET, "/whoami").permitAll()
        .antMatchers("/onboarding").permitAll()
        .antMatchers(GET, "/users/*").permitAll()
        .antMatchers("/onboardingInitiation").permitAll()
        .antMatchers(POST, "/preUsers").permitAll()
        //Authentication check done in controller for legalFiles
        .antMatchers(GET, "/users/*/legalFiles").permitAll()
        .antMatchers(PUT, "/users/*/legalFiles/*").permitAll()
        .antMatchers(OPTIONS, "/**").permitAll()
        .antMatchers(GET, "/whois/*").permitAll()
        .requestMatchers(
            new SelfAccountMatcher(GET, "/accounts/*/customers", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(POST, "/accounts/*/customers", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(PUT, "/accounts/*/customers", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(GET, "/accounts/*/customers/*", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(POST, "/accounts/*/customers/upload", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(PUT, "/accounts/{id}/customers/status", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(GET, "/accounts/*/transactions",
                authResourceProvider)).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(GET, "/accounts/*/transactions/*",
                authResourceProvider)).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(PUT, "/accounts/*/transactions/*/invoices/*",
                authResourceProvider)).authenticated()
        .requestMatchers(new SelfUserMatcher(GET, "/users/*/accounts", authResourceProvider))
        .authenticated()
        .requestMatchers(
            new SelfUserAccountMatcher(GET, "/users/*/accounts/*/accountHolders",
                authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfUserAccountMatcher(
                POST, "/users/*/accounts/*/initiateAccountValidation", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfUserAccountMatcher(PUT, "/users/*/accounts/*/identity", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfUserAccountMatcher(POST, "/users/*/accounts/*/active", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfUserAccountMatcher(POST, "/users/*/accounts/*/initiateBankConnection",
                authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfUserMatcher(POST, "/users/*/initiateBankConnection", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfUserMatcher(POST, "/users/*/disconnectBank", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(GET, "/accounts/*/invoices/*", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(PUT, "/accounts/*/invoices/*", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(PUT, "/accounts/*/invoices/archive", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(GET, "/accounts/*/products", authResourceProvider)
        ).authenticated()
        .requestMatchers(new SelfAccountMatcher(GET, "/accounts/*/products/*", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(PUT, "/accounts/*/products", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(POST, "/accounts/*/products", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(POST, "/accounts/*/products/upload", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(PUT, "/accounts/{aId}/products/status", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(GET, "/accounts/*/invoices", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(POST, "/accounts/*/paymentInitiations", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(GET, "/accounts/*/transactionCategories", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(POST, "/accounts/*/transactions/*/transactionCategories",
                authResourceProvider)
        ).authenticated()
        .antMatchers(GET, "/users").authenticated()
        .requestMatchers(
            new SelfAccountMatcher(GET, "/accounts/*/files/*", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(POST, "/accounts/*/files/*/raw", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(GET, "/accounts/*/files/*/raw", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(GET, "/accounts/*/marketplaces", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(GET, "/accounts/*/transactionsSummary", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(GET, "/accounts/*/invoiceRelaunchConf", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(PUT, "/accounts/*/invoiceRelaunchConf", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(GET, "/accounts/*/invoices/*/relaunches", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(POST, "/accounts/*/invoices/*/relaunch", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(
                GET,
                "/accounts/*/invoices/*/relaunchConf",
                authResourceProvider
            )
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(
                PUT,
                "/accounts/*/invoices/*/relaunchConf",
                authResourceProvider
            )
        ).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(
                POST,
                "/accounts/*/invoices/*/duplication",
                authResourceProvider
            )
        ).authenticated()
        .antMatchers(GET, "/businessActivities").authenticated()
        //TODO: set SelfUserAccountHolderMatcher
        .antMatchers(
            PUT, "/users/*/accounts/*/accountHolders/*/businessActivities").authenticated()
        .antMatchers(
            PUT, "/users/*/accounts/*/accountHolders/*/revenueTargets").authenticated()
        .antMatchers(
            PUT, "/users/*/accounts/*/accountHolders/*/companyInfo").authenticated()
        .antMatchers(
            PUT, "/users/*/accounts/*/accountHolders/*/globalInfo").authenticated()
        .antMatchers(
            PUT, "/users/*/accountHolders/*/feedback/*").authenticated()
        .antMatchers(
            POST, "/users/*/accountHolders/*/feedback").authenticated()
        .requestMatchers(
            new SelfAccountMatcher(GET, "/accounts/*/paymentRequests", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfAccountHolderMatcher(GET, "/accountHolders/*/prospects", authResourceProvider))
        .authenticated()
        .requestMatchers(
            new SelfAccountHolderMatcher(PUT, "/accountHolders/*/prospects/*/prospectConversion",
                authResourceProvider))
        .authenticated()
        .requestMatchers(
            new SelfAccountHolderMatcher(PUT, "/accountHolders/*/prospects",
                authResourceProvider))
        .authenticated()
        .requestMatchers(
            new SelfAccountHolderMatcher(POST, "/accountHolders/*/prospects/prospectsEvaluation",
                authResourceProvider))
        .hasAnyRole(EVAL_PROSPECT.getRole())
        .requestMatchers(
            new SelfUserMatcher(GET, "/users/*/calendars/*/events", authResourceProvider)
        ).authenticated()
        .requestMatchers(
            new SelfUserMatcher(PUT, "/users/*/calendars/*/events", authResourceProvider))
        .authenticated()
        .requestMatchers(
            new SelfUserMatcher(POST, "/users/*/calendars/oauth2/consent", authResourceProvider))
        .authenticated()
        .requestMatchers(new SelfUserMatcher(GET, "/users/*/calendars", authResourceProvider))
        .authenticated()
        .requestMatchers(
            new SelfUserMatcher(POST, "/users/*/calendars/oauth2/auth", authResourceProvider))
        .authenticated()
        .requestMatchers(
            new SelfAccountMatcher(PUT, "/accounts/*/invoices/{iId}/paymentRegulations/*/paymentMethod", authResourceProvider))
        .authenticated()
        .antMatchers("/**").denyAll()

        // disable superfluous protections
        // Eg if all clients are non-browser then no csrf
        // https://docs.spring.io/spring-security/site/docs/3.2.0.CI-SNAPSHOT/reference/html/csrf.html,
        // Sec 13.3
        .and()
        .csrf().disable() // NOSONAR
        .formLogin().disable()
        .logout().disable();
    // formatter:on
  }

  private Exception forbiddenWithRemoteInfo(Exception e, HttpServletRequest req) {
    log.info(String.format(
        "Access is denied for remote caller: address=%s, host=%s, port=%s",
        req.getRemoteAddr(), req.getRemoteHost(), req.getRemotePort()));
    return new ForbiddenException(e.getMessage());
  }

  private BearerAuthFilter bearerFilter(RequestMatcher requestMatcher) throws Exception {
    BearerAuthFilter bearerFilter = new BearerAuthFilter(requestMatcher, AUTHORIZATION_HEADER);
    bearerFilter.setAuthenticationManager(authenticationManager());
    bearerFilter.setAuthenticationSuccessHandler(
        (httpServletRequest, httpServletResponse, authentication) -> {
        });
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
}
