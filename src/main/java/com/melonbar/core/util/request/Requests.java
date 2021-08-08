package com.melonbar.core.util.request;

import com.melonbar.core.http.request.BaseRequest;
import com.melonbar.core.util.Format;

public final class Requests {

    public static String toString(final BaseRequest request) {
        return request == null ? "null" : Format.format("{}(uri={}, method={}, body={})",
                request.getClass().getSimpleName(),
                request.getUri(),
                request.getMethod(),
                request.getBody());
    }
}
