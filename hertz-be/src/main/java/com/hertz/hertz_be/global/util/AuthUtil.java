package com.hertz.hertz_be.global.util;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

public class AuthUtil {

    public static void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, long maxAgeSeconds, boolean isLocal) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .maxAge(maxAgeSeconds)
                .path("/")
                .sameSite("None")
                .domain(isLocal ? ".hertz-tuning.com" : null)
                .httpOnly(true)
                .secure(!isLocal)
                .build();

        response.setHeader("Set-Cookie", cookie.toString());
    }
}
