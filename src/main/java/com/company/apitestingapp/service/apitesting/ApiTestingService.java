package com.company.apitestingapp.service.apitesting;

import com.company.apitestingapp.entity.*;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class ApiTestingService {
    private static final Logger log = LoggerFactory.getLogger(ApiTestingService.class);

    @Autowired
    private DataManager dataManager;
    @Autowired
    private RestTemplate restTemplate;

    public RequestExecution executionRequest(Request request) {
        long startTime = System.currentTimeMillis();
        RequestExecution execution = dataManager.create(RequestExecution.class);

        ResponseEntity<String> response = null;

        try {
            //проверяем, существует ли уже такой запрос в базе данных
            Optional<Request> existingRequest = findExistingRequest(request);
            Request requestToUse;

            //если сушествует используем его
            if (existingRequest.isPresent()) {
                requestToUse = existingRequest.get();
            } else if (request.getId() != null) {
                //используем переданный запрос (уже сохранен)
                requestToUse = request;
            } else {
                //иначе сохраняем новый запрос
                requestToUse = saveNewRequest(request);
            }

            execution.setRequest(requestToUse);
            execution.setExecutedAt(OffsetDateTime.now());

            if (!isValidUrl(requestToUse.getUrl())) {
                throw new IllegalArgumentException("Неверный формат URL: " + requestToUse.getUrl());
            }

            String fullUrl = buildFullUrl(requestToUse);
            HttpHeaders headers = buildHeaders(requestToUse);

            HttpEntity<String> requestEntity = new HttpEntity<>(
                    requestToUse.getBody(),
                    headers
            );
            try {
                response = restTemplate.exchange(
                        fullUrl,
                        HttpMethod.valueOf(requestToUse.getMethod().name()),
                        requestEntity,
                        String.class
                );

                execution.setStatusCode(response.getStatusCode().value());
                execution.setResponseHeaders(formatResponseHeaders(response.getHeaders()));
                execution.setIsSuccess(response.getStatusCode().is2xxSuccessful());

                if (requestToUse.getMethod() == Method.HEAD) {
                    execution.setResponseBody("");
                } else {
                    execution.setResponseBody(response.getBody());
                }
            } catch (HttpClientErrorException | HttpServerErrorException e){
                // Обрабатываем HTTP ошибки (4xx, 5xx)
                execution.setStatusCode(e.getStatusCode().value());
                execution.setResponseHeaders(formatResponseHeaders(e.getResponseHeaders()));
                execution.setIsSuccess(false);
                execution.setResponseBody(e.getResponseBodyAsString());
                execution.setErrorMessage(e.getStatusText());
            } catch (RestClientException e){
                execution.setStatusCode(StatusCode.INTERNAL_SERVER_ERROR.getCode());
                execution.setIsSuccess(false);
                execution.setErrorMessage(e.getMessage());
            }

        } catch (Exception e) {
            execution.setErrorMessage(e.getMessage());
            execution.setIsSuccess(false);
            execution.setStatusCode(StatusCode.INTERNAL_SERVER_ERROR.getCode());
        } finally {
            long responseTime = System.currentTimeMillis() - startTime;
            execution.setResponseTimeMs(responseTime);

            // Расчет размера ответа
            long sizeBytes = 0L;
            if (request.getMethod() == Method.HEAD && response != null) {
                String contentLength = response.getHeaders().getFirst("Content-Length");
                if (contentLength != null) {
                    try {
                        sizeBytes = Long.parseLong(contentLength);
                    } catch (NumberFormatException ex) {
                        sizeBytes = 0L;
                    }
                }
            } else if (execution.getResponseBody() != null) {
                sizeBytes = execution.getResponseBody().getBytes(StandardCharsets.UTF_8).length;
            }
            execution.setSizeBytes(sizeBytes);

            dataManager.save(execution);
        }

        return execution;
    }

    private Request saveNewRequest(Request request) {
        SaveContext saveContext = new SaveContext();
        saveContext.saving(request);

        if (request.getHeaders() != null) {
            for (Header header : request.getHeaders()) {
                header.setRequest(request);
                saveContext.saving(header);
            }
        }

        if (request.getQueryParams() != null) {
            for (QueryParameter param : request.getQueryParams()) {
                param.setRequest(request);
                saveContext.saving(param);
            }
        }

        dataManager.save(saveContext);
        return request;
    }

    private String formatResponseHeaders(HttpHeaders headers) {
        if (headers == null || headers.isEmpty()) {
            return "";
        }
        return headers.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + String.join(", ", entry.getValue()))
                .collect(Collectors.joining("\n"));
    }

    private String buildFullUrl(Request request) {
        String baseUrl = request.getUrl();

        if (request.getQueryParams() == null || request.getQueryParams().isEmpty()) {
            return baseUrl;
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);

        for (QueryParameter param : request.getQueryParams()) {
            if (param.getKey() != null && !param.getKey().trim().isEmpty()) {
                builder.queryParam(param.getKey(), param.getValue());
            }
        }

        return builder.toUriString();
    }

    private HttpHeaders buildHeaders(Request request) {
        HttpHeaders headers = new HttpHeaders();

        if (request.getBodyType() != null) {
            switch (request.getBodyType()) {
                case XML:
                    headers.setContentType(MediaType.APPLICATION_XML);
                    break;
                case TEXT:
                    headers.setContentType(MediaType.TEXT_PLAIN);
                    break;
                case FORM_DATA:
                    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                    break;
                case JSON:
                default:
                    headers.setContentType(MediaType.APPLICATION_JSON);
            }
        }

        if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
            for (Header header : request.getHeaders()) {
                if (header.getKey() != null && !header.getKey().trim().isEmpty()) {
                    headers.add(header.getKey(), header.getValue());
                }
            }
        }

        return headers;
    }

    private boolean isValidUrl(String url){
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            new java.net.URL(url).toURI();
            return true;
        } catch (Exception e){
            return false;
        }
    }

    public Optional<Request> findExistingRequest(Request request) {
        try {
            return dataManager.load(Request.class)
                    .query("select r from Request r where " +
                            "r.url = :url and " +
                            "r.method = :method and " +
                            "r.bodyType = :bodyType and " +
                            "r.body = :body")
                    .parameter("url", request.getUrl())
                    .parameter("method", request.getMethod())
                    .parameter("bodyType", request.getBodyType())
                    .parameter("body", request.getBody())
                    .optional();
        } catch (Exception e) {
            log.debug("Error finding existing request", e);
            return Optional.empty();
        }
    }
}
