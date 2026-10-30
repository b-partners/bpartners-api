package app.bpartners.api.service.utils;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Component
public class TemplateResolverEngine {
  // Built once: the engine caches parsed templates, so rebuilding it per call re-parsed every
  // template on each render.
  private final TemplateEngine templateEngine = createTemplateEngine();

  public String parseTemplateResolver(String template, Context context) {
    return templateEngine.process(template, context);
  }

  @NotNull
  public TemplateEngine getTemplateEngine() {
    return templateEngine;
  }

  private static TemplateEngine createTemplateEngine() {
    ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
    templateResolver.setPrefix("/templates/");
    templateResolver.setSuffix(".html");
    templateResolver.setCharacterEncoding("UTF-8");
    templateResolver.setTemplateMode(TemplateMode.HTML);

    TemplateEngine templateEngine = new TemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);
    return templateEngine;
  }
}
