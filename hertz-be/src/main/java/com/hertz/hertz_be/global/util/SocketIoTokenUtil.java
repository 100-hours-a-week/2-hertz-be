package com.hertz.hertz_be.global.util;

import org.springframework.stereotype.Component;

@Component
public class SocketIoTokenUtil {
    public String extractCookie(String cookieHeader, String key) {
        if (cookieHeader == null) return null;

        for (String cookie : cookieHeader.split(";")) {
            String[] pair = cookie.trim().split("=");
            if (pair.length == 2 && pair[0].equals(key)) {
                return pair[1];
            }
        }
        return null;
    }
}
