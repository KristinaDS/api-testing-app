package com.company.apitestingapp.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class HttpService {

    private final RestTemplate restTemplate;

    public HttpService() {
        this.restTemplate = new RestTemplate();
    }

    public HttpResponse execute(HttpRequest request) {
        try {
            // Создаем HttpHeaders
            HttpHeaders headers = new HttpHeaders();

            // Добавляем пользовательские заголовки
            if (request.getHeaders() != null) {
                request.getHeaders().forEach(headers::set);
            }

            // Устанавливаем Content-Type по умолчанию для запросов с телом
            if (request.getBody() != null && !request.getBody().trim().isEmpty()) {
                if (request.getHeaders() == null || !request.getHeaders().containsKey("Content-Type")) {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                }
            }

            // Создаем HttpEntity
            HttpEntity<String> entity = new HttpEntity<>(request.getBody(), headers);

            // Определяем HTTP метод - используем явное преобразование вместо resolve
            HttpMethod httpMethod = getHttpMethod(request.getMethod());

            // Выполняем запрос
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    request.getUrl(),
                    httpMethod,
                    entity,
                    String.class
            );

            // Собираем ответ
            return HttpResponse.builder()
                    .statusCode(responseEntity.getStatusCode().value())
                    .body(responseEntity.getBody())
                    .headers(convertHeaders(responseEntity.getHeaders()))
                    .build();

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // Обработка HTTP ошибок 4xx и 5xx
            return HttpResponse.builder()
                    .statusCode(e.getStatusCode().value())
                    .body(e.getResponseBodyAsString())
                    .headers(convertHeaders(e.getResponseHeaders()))
                    .build();

        } catch (Exception e) {
            // Обработка других ошибок (таймауты, сетевые проблемы и т.д.)
            throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    private HttpMethod getHttpMethod(String method) {
        String upperMethod = method.toUpperCase();
        switch (upperMethod) {
            case "GET":
                return HttpMethod.GET;
            case "POST":
                return HttpMethod.POST;
            case "PUT":
                return HttpMethod.PUT;
            case "PATCH":
                return HttpMethod.PATCH;
            case "DELETE":
                return HttpMethod.DELETE;
            case "HEAD":
                return HttpMethod.HEAD;
            case "OPTIONS":
                return HttpMethod.OPTIONS;
            default:
                throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }
    }

    private Map<String, String> convertHeaders(HttpHeaders httpHeaders) {
        if (httpHeaders == null) {
            return Map.of();
        }

        Map<String, String> result = new java.util.HashMap<>();
        httpHeaders.forEach((key, values) -> {
            if (!values.isEmpty()) {
                result.put(key, String.join(", ", values));
            }
        });
        return result;
    }
}