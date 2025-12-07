package com.company.apitestingapp.service;

import java.util.Map;

public class HttpRequest {
    private String method;
    private String url;
    private Map<String, String> headers;
    private String body;

    public static Builder builder() {
        return new Builder();
    }

    public String getMethod() { return method; }
    public String getUrl() { return url; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody() { return body; }

    public static class Builder {
        private HttpRequest request = new HttpRequest();

        public Builder method(String method) {
            request.method = method;
            return this;
        }

        public Builder url(String url) {
            request.url = url;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            request.headers = headers;
            return this;
        }

        public Builder body(String body) {
            request.body = body;
            return this;
        }

        public HttpRequest build() {
            return request;
        }
    }
}
