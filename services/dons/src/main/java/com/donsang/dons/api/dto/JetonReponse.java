package com.donsang.dons.api.dto;

/** Format habituel OAuth2 : accessToken, type et durée de validité en secondes. */
public record JetonReponse(String accessToken, String tokenType, long expiresIn) {
}
