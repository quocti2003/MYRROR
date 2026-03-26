package com.mirror.product.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class RecipientExtractor {

    private RecipientExtractor() {
    }

    public static Set<String> resolveRecipients(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return Collections.emptySet();
        }
        Object explicit = payload.getOrDefault("recipients", payload.get("recipient"));
        if (explicit instanceof String str) {
            return Collections.singleton(str);
        }
        if (explicit instanceof Collection<?> collection) {
            Set<String> recipients = new LinkedHashSet<>();
            collection.stream()
                    .map(Object::toString)
                    .forEach(recipients::add);
            return recipients;
        }
        return Collections.emptySet();
    }
}
