package com.chaerok.backend.auth.oauth.verifier;

import com.chaerok.backend.auth.oauth.dto.OAuthUserInfo;
import com.chaerok.backend.user.entity.OAuthProvider;

public interface OAuthTokenVerifier {

    OAuthProvider getProvider();

    OAuthUserInfo verify(String idToken, String nonce);
}