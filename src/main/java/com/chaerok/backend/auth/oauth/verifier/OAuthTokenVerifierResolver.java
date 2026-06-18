package com.chaerok.backend.auth.oauth.verifier;

import com.chaerok.backend.user.entity.OAuthProvider;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class OAuthTokenVerifierResolver {

    private final Map<OAuthProvider, OAuthTokenVerifier> verifiers;

    public OAuthTokenVerifierResolver(List<OAuthTokenVerifier> verifiers) {
        this.verifiers = new EnumMap<>(OAuthProvider.class);

        for (OAuthTokenVerifier verifier : verifiers) {
            this.verifiers.put(verifier.getProvider(), verifier);
        }
    }

    public OAuthTokenVerifier resolve(OAuthProvider provider) {
        OAuthTokenVerifier verifier = verifiers.get(provider);

        if (verifier == null) {
            throw new IllegalStateException(
                    "OAuth 검증기가 등록되지 않았습니다: " + provider
            );
        }

        return verifier;
    }
}