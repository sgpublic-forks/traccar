package org.traccar.helper;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import java.util.TreeMap;

public class SignedRequestProvider extends TreeMap<String, Object> {
    private final WebTarget request;
    private final String path;
    private final String secretKey;
    private final boolean signNeedUrlEncode;

    public SignedRequestProvider(String secretKey, Client client, String url) {
        this(secretKey, client, url, true);
    }
    public SignedRequestProvider(String secretKey, Client client, String url, boolean signNeedUrlEncode) {
        this.request = client.target(url);
        this.path = request.getUri().getPath();
        this.secretKey = secretKey;
        this.signNeedUrlEncode = signNeedUrlEncode;
    }

    @Override
    public Object put(String key, Object value) {
        request.queryParam(key, value);
        return super.put(key, convertValueForSign(value.toString()));
    }

    public String convertValueForSign(String value) {
        if (signNeedUrlEncode) {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } else {
            return value;
        }
    }

    private String createSign(boolean signWithPath) {
        StringBuilder queryString = new StringBuilder();

        if (signWithPath) {
            queryString.append(path);
        }
        if (!isEmpty()) {
            queryString.append("?");

            for (Entry<?, ?> pair : entrySet()) {
                queryString.append(pair.getKey())
                        .append("=")
                        .append(pair.getValue())
                        .append("&");
            }
        }

        return StringUtil.toMD5(queryString + secretKey);
    }

    public Invocation.Builder request(String signKeyName) {
        return request(signKeyName, true);
    }
    public Invocation.Builder request(String signKeyName, boolean signWithPath) {
        if (secretKey != null) {
            put(signKeyName, createSign(signWithPath));
        }
        return request.request();
    }
}
