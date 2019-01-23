package io.bspk.oauth.xyz.rewrite;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RewriteWebConfig implements WebMvcConfigurer {

    private final RewriteProperties rewriteProperties;

    public RewriteWebConfig(RewriteProperties rewriteProperties) {
        this.rewriteProperties = rewriteProperties;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        rewriteProperties.getEntries().forEach(
                entry -> {
                    LoggerFactory.getLogger(RewriteWebConfig.class).info("Mapping source url '{}' to target '{}'",
                            entry.getSourceUrl(), entry.getTarget());
                    registry.addViewController(entry.getSourceUrl()).setViewName(entry.getTarget());
                }
        );
    }
}