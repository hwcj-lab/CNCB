package com.example.bioplatform.research.collection.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/**
 * An explicit bank-to-source route proves ownership; relation edges alone never do.
 */
@Component
public class SourceMembership {
    private final ObjectMapper json;

    public SourceMembership(ObjectMapper json) {
        this.json = json;
    }

    public boolean belongs(String code, String payload, Map<String, String> routes) {
        if (payload == null || payload.isBlank()) return false;
        try {
            var banks = json.readTree(payload);
            if (banks == null || !banks.isArray()) return false;
            for (var bank : banks) {
                var target = routes.get(bank.path("db").asText("").trim().toLowerCase(Locale.ROOT));
                if (code.equalsIgnoreCase(target)) return true;
            }
            return false;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid cncbBanks JSON", ex);
        }
    }
}
