package com.company.apitestingapp.service;

import java.util.Map;

public class HttpResponse {
    private int statusCode;
    private String body;
    private Map<String, String> headers;

    public static Builder builder() {
        return new Builder();
    }

    public int getStatusCode() { return statusCode; }
    public String getBody() { return body; }
    public Map<String, String> getHeaders() { return headers; }

    public static class Builder {
        private HttpResponse response = new HttpResponse();

        public Builder statusCode(int statusCode) {
            response.statusCode = statusCode;
            return this;
        }

        public Builder body(String body) {
            response.body = body;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            response.headers = headers;
            return this;
        }

        public HttpResponse build() {
            return response;
        }
    }
}
