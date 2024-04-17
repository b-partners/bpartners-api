package app.bpartners.api.endpoint.rest.security;

import app.bpartners.api.endpoint.rest.security.exception.UnapprovedLegalFileException;
import app.bpartners.api.model.LegalFile;
import app.bpartners.api.model.User;
import app.bpartners.api.service.LegalFileService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

public class LegalFilesApprovalFilter extends OncePerRequestFilter {
  private final RequestMatcher requiresLegalFilesCheckRequestMatcher;
  private final LegalFileService legalFileService;
  private final AuthenticatedResourceProvider authenticatedResourceProvider;

  public LegalFilesApprovalFilter(
      RequestMatcher requiresLegalFilesCheckRequestMatcher,
      LegalFileService legalFileService,
      AuthenticatedResourceProvider authenticatedResourceProvider) {
    super();
    this.requiresLegalFilesCheckRequestMatcher = requiresLegalFilesCheckRequestMatcher;
    this.legalFileService = legalFileService;
    this.authenticatedResourceProvider = authenticatedResourceProvider;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !requiresLegalFilesCheckRequestMatcher.matches(request);
  }

  @Override
  protected void doFilterInternal(
      @NotNull HttpServletRequest request,
      @NotNull HttpServletResponse response,
      FilterChain filterChain)
      throws ServletException, IOException {
    var authenticatedUser = authenticatedResourceProvider.getUser();
    List<LegalFile> legalFilesList =
        legalFileService.getAllToBeApprovedLegalFilesByUserId(authenticatedUser.getId());
    checkLegalFiles(legalFilesList, authenticatedUser);
    filterChain.doFilter(request, response);
  }

  private void checkLegalFiles(List<LegalFile> legalFiles, User user) {
    if (!legalFiles.isEmpty()) {
      StringBuilder exceptionMessageBuilder = new StringBuilder();
      legalFiles.forEach(
          legalFile -> {
            if (!legalFile.isApproved()) {
              exceptionMessageBuilder
                  .append("User.")
                  .append(user.getId())
                  .append(" has not approved the legal file ")
                  .append(legalFile.getName());
            }
          });
      String exceptionMessage = exceptionMessageBuilder.toString();
      if (!exceptionMessage.isEmpty()) {
        throw new UnapprovedLegalFileException(exceptionMessage);
      }
    }
  }
}
