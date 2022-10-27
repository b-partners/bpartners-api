package app.bpartners.api.endpoint.rest.security;

import app.bpartners.api.endpoint.rest.security.matcher.SelfAccountMatcher;
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

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

@Configuration
@Slf4j
public class SecurityConf extends WebSecurityConfigurerAdapter {

  private static final String AUTHORIZATION_HEADER = "Authorization";
  private final AuthProvider authProvider;
  private final HandlerExceptionResolver exceptionResolver;

  public SecurityConf(
      AuthProvider authProvider,
      // InternalToExternalErrorHandler behind
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
    this.authProvider = authProvider;
    this.exceptionResolver = exceptionResolver;
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
                .resolveException(req, res, null, forbiddenWithRemoteInfo(req)))
        .accessDeniedHandler(
            // note(spring-exception): issues like when a user not having required roles
            (req, res, e) -> exceptionResolver
                .resolveException(req, res, null, forbiddenWithRemoteInfo(req)))

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
                    new AntPathRequestMatcher("/onboardingInitiation", POST.name()),
                    new AntPathRequestMatcher("/**", OPTIONS.toString())
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
        .antMatchers("/onboardingInitiation").permitAll()
        .antMatchers(POST, "/preUsers").permitAll()
        .antMatchers(OPTIONS, "/**").permitAll()
        .requestMatchers(new SelfAccountMatcher(GET, "/accounts/*/customers")).authenticated()
        .requestMatchers(new SelfAccountMatcher(POST, "/accounts/*/customers")).authenticated()
        .antMatchers(GET, "/accounts/*/transactions").authenticated()
        .requestMatchers(new SelfUserMatcher(GET, "/users/*/accounts")).authenticated()
        .antMatchers(GET, "/users/*/accounts/*/accountHolders").authenticated()
        .requestMatchers(new SelfAccountMatcher(GET, "/accounts/*/invoices/*")).authenticated()
        .requestMatchers(new SelfAccountMatcher(PUT, "/accounts/*/invoices/*")).authenticated()
        .requestMatchers(new SelfAccountMatcher(GET, "/accounts/*/products")).authenticated()
        .requestMatchers(new SelfAccountMatcher(GET, "/accounts/*/invoices")).authenticated()
        .requestMatchers(new SelfAccountMatcher(POST, "/accounts/*/invoices/*/products"))
        .authenticated()
        .requestMatchers(new SelfAccountMatcher(POST, "/accounts/*/paymentInitiations"))
        .authenticated()
        .requestMatchers(new SelfAccountMatcher(GET, "/accounts/*/transactionCategories"))
        .authenticated()
        .requestMatchers(new SelfAccountMatcher(POST,
            "/accounts/*/transactions/*/transactionCategories")).authenticated()
        .antMatchers(GET, "/whoami").authenticated()
        .antMatchers(GET, "/preUsers").authenticated()
        .antMatchers(GET, "/whoami").authenticated()
        .antMatchers(GET, "/users").authenticated()
        .antMatchers(GET, "/users/*").authenticated()
        .requestMatchers(new SelfAccountMatcher(GET, "/accounts/*/files/*")).authenticated()
        .requestMatchers(new SelfAccountMatcher(POST, "/accounts/*/files/*/raw")).authenticated()
        .requestMatchers(new SelfAccountMatcher(GET, "/accounts/*/files/*/raw")).authenticated()
        .requestMatchers(new SelfAccountMatcher(GET, "/accounts/*/marketplaces")).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(GET, "/accounts/*/invoiceRelaunchConf")).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(PUT, "/accounts/*/invoiceRelaunchConf")).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(GET, "/accounts/*/invoices/*/relaunches")).authenticated()
        .requestMatchers(
            new SelfAccountMatcher(POST, "/accounts/*/invoices/*/relaunches")).authenticated()
        .antMatchers(GET, "/businessActivities").authenticated()
        .antMatchers(PUT, "/users/*/accounts/*/accountHolders/*/businessActivities").authenticated()
        .antMatchers(PUT, "/users/*/accounts/*/accountHolders/*/companyInfo").authenticated()
        //TODO: which SelfMatcher should I put here ?. And on line 87 ?
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

  private Exception forbiddenWithRemoteInfo(HttpServletRequest req) {
    log.info(String.format(
        "Access is denied for remote caller: address=%s, host=%s, port=%s",
        req.getRemoteAddr(), req.getRemoteHost(), req.getRemotePort()));
    return new ForbiddenException("Access is denied");
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
            exceptionResolver.resolveException(req, res, null, forbiddenWithRemoteInfo(req)));
    return bearerFilter;
  }
}
