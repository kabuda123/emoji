package com.company.emoji.auth;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EmailCodeStore {
    private final Map<String, StoredCode> codes = new ConcurrentHashMap<>();

    public void put(String email, String code, long ttlSeconds) {
        codes.put(normalize(email), new StoredCode(code, Instant.now().plusSeconds(ttlSeconds)));
    }

    public boolean matches(String email, String code) {
        StoredCode storedCode = codes.get(normalize(email));
        if (storedCode == null || Instant.now().isAfter(storedCode.expiresAt())) {
            return false;
        }
        return storedCode.code().equals(code);
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private record StoredCode(String code, Instant expiresAt) {
    }
}