package com.mirror.product.service.notification;

import com.mirror.product.config.notification.NotificationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
public class EmailTemplateRenderer {

    private static final Logger log = LoggerFactory.getLogger(EmailTemplateRenderer.class);

    private final TemplateEngine templateEngine;
    private final NotificationProperties properties;

    public EmailTemplateRenderer(TemplateEngine templateEngine, NotificationProperties properties) {
        this.templateEngine = templateEngine;
        this.properties = properties;
    }

    public String renderHtml(String templateKey, Map<String, Object> model) {
        String template = properties.getTemplates().getIds().get(templateKey);
        if (template == null) {
            log.warn("No template configured for key {}. Using fallback plain body.", templateKey);
            return buildFallbackBody(model);
        }

        Context context = new Context();
        context.setVariables(model);
        return templateEngine.process(template, context);
    }

    public String buildFallbackBody(Map<String, Object> model) {
        return model.toString();
    }
}
