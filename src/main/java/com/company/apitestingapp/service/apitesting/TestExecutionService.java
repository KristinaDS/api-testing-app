package com.company.apitestingapp.service.apitesting;

import com.company.apitestingapp.entity.*;
import com.company.apitestingapp.entity.Collection;
import com.company.apitestingapp.service.HttpRequest;
import com.company.apitestingapp.service.HttpResponse;
import com.company.apitestingapp.service.HttpService;
import io.jmix.core.DataManager;
import io.jmix.core.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TestExecutionService {

    private static final Logger log = LoggerFactory.getLogger(TestExecutionService.class);

    @Autowired
    private DataManager dataManager;
    @Autowired
    private Metadata metadata;
    @Autowired
    private HttpService httpService;

    @Transactional
    public TestExecution runTestSuite(Collection collection) {
        log.info("Starting test suite execution for collection: {}", collection.getName());

        // Создаем запись о выполнении тестов
        TestExecution testExecution = metadata.create(TestExecution.class);
        testExecution.setName("Test Run - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        testExecution.setCollection(collection);
        testExecution.setTotalTests(0);
        testExecution.setPassedTests(0);
        testExecution.setFailedTests(0);
        testExecution.setStatus("RUNNING");
        testExecution.setCreateTs(LocalDateTime.now());

        TestExecution savedExecution = dataManager.save(testExecution);
        UUID executionId = savedExecution.getId();

        long startTime = System.currentTimeMillis();

        try {
            // Загружаем запросы из коллекции
            List<Request> requests = dataManager.load(Request.class)
                    .query("select r from Request r where r.collection = :collection")
                    .parameter("collection", collection)
                    .list();

            log.info("Found {} requests in collection", requests.size());

            if (requests.isEmpty()) {
                createNoTestsResult(executionId);
                updateExecutionNoTests(executionId, System.currentTimeMillis() - startTime);
                return dataManager.load(TestExecution.class).id(executionId).one();
            }

            updateExecutionTotalTests(executionId, requests.size());

            List<TestExecutionResult> results = new ArrayList<>();
            int passed = 0;
            int failed = 0;
            int errors = 0;

            // Выполняем каждый запрос
            for (int i = 0; i < requests.size(); i++) {
                Request request = requests.get(i);
                log.info("Executing request {}/{}: {} {}", i + 1, requests.size(), request.getMethod(), request.getUrl());

                TestExecutionResult result = executeSingleRequest(request, executionId);

                if ("PASSED".equals(result.getStatus())) {
                    passed++;
                    log.info("Request {} PASSED", request.getName());
                } else if ("FAILED".equals(result.getStatus())) {
                    failed++;
                    log.info("Request {} FAILED: {}", request.getName(), result.getErrorMessage());
                } else if ("ERROR".equals(result.getStatus())) {
                    errors++;
                    failed++;
                    log.info("Request {} ERROR: {}", request.getName(), result.getErrorMessage());
                }

                // Обновляем прогресс каждые 5 запросов или в конце
                if ((i + 1) % 5 == 0 || i == requests.size() - 1) {
                    updateExecutionProgress(executionId, passed, failed);
                }
            }

            // Финальный статус
            String finalStatus = failed == 0 ? "PASSED" : "FAILED";
            if (errors > 0) {
                finalStatus = "ERROR";
            }

            updateExecutionFinalStatus(executionId, finalStatus, passed, failed, System.currentTimeMillis() - startTime);

            log.info("Test suite execution completed: {} passed, {} failed", passed, failed);

            return dataManager.load(TestExecution.class).id(executionId).one();

        } catch (Exception e) {
            log.error("Error executing test suite", e);
            updateExecutionError(executionId, e.getMessage(), System.currentTimeMillis() - startTime);
            return dataManager.load(TestExecution.class).id(executionId).one();
        }
    }

    private void createNoTestsResult(UUID testExecutionId) {
        TestExecution testExecution = dataManager.load(TestExecution.class).id(testExecutionId).one();

        TestExecutionResult result = metadata.create(TestExecutionResult.class);
        result.setTestExecution(testExecution);
        result.setStatus("NO_TESTS");
        result.setErrorMessage("В коллекции нет тестов для выполнения");
        result.setExecutionTime(0L);

        dataManager.save(result);
    }

    private TestExecutionResult executeSingleRequest(Request request, UUID testExecutionId) {
        TestExecution testExecution = dataManager.load(TestExecution.class).id(testExecutionId).one();

        TestExecutionResult result = metadata.create(TestExecutionResult.class);
        result.setTestExecution(testExecution);
        result.setRequest(request);
        result.setStatus("ERROR");

        long startTime = System.currentTimeMillis();

        try {
            // Валидация URL
            if (!isValidUrl(request.getUrl())) {
                result.setErrorMessage("Invalid URL: " + request.getUrl());
                result.setStatus("FAILED");
                result.setExecutionTime(System.currentTimeMillis() - startTime);
                return dataManager.save(result);
            }

            // Подготовка и выполнение HTTP-запроса
            HttpRequest httpRequest = prepareHttpRequest(request);
            HttpResponse response = httpService.execute(httpRequest);

            long endTime = System.currentTimeMillis();
            result.setExecutionTime(endTime - startTime);
            result.setResponseStatus(response.getStatusCode());
            result.setResponseBody(response.getBody());
            result.setResponseHeaders(formatHeaders(response.getHeaders()));

            // Проверка статуса ответа
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                result.setStatus("PASSED");
            } else {
                result.setStatus("FAILED");
                result.setErrorMessage("HTTP Status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error executing request: {}", request.getUrl(), e);
            result.setErrorMessage(e.getMessage());
            result.setStatus("ERROR");
            result.setExecutionTime(System.currentTimeMillis() - startTime);
        }

        return dataManager.save(result);
    }

    private boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            new URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private HttpRequest prepareHttpRequest(Request request) {
        String finalUrl = buildFinalUrl(request);
        Map<String, String> headers = buildHeaders(request);
        String body = getRequestBody(request);

        return HttpRequest.builder()
                .method(request.getMethod().getId().toUpperCase())
                .url(finalUrl)
                .headers(headers)
                .body(body)
                .build();
    }

    private String buildFinalUrl(Request request) {
        String baseUrl = request.getUrl();

        // Добавляем query parameters если есть
        if (request.getQueryParams() != null && !request.getQueryParams().isEmpty()) {
            StringBuilder urlBuilder = new StringBuilder(baseUrl);
            boolean firstParam = true;

            for (QueryParameter param : request.getQueryParams()) {
                if (param.getKey() != null && !param.getKey().trim().isEmpty()) {
                    if (firstParam) {
                        urlBuilder.append("?");
                        firstParam = false;
                    } else {
                        urlBuilder.append("&");
                    }
                    urlBuilder.append(param.getKey())
                            .append("=")
                            .append(param.getValue() != null ? param.getValue() : "");
                }
            }
            return urlBuilder.toString();
        }

        return baseUrl;
    }

    private Map<String, String> buildHeaders(Request request) {
        Map<String, String> headers = new HashMap<>();

        // Добавляем стандартные заголовки
        headers.put("User-Agent", "API-Testing-App/1.0");
        headers.put("Accept", "*/*");

        // Добавляем пользовательские заголовки
        if (request.getHeaders() != null) {
            for (Header header : request.getHeaders()) {
                if (header.getKey() != null && !header.getKey().trim().isEmpty()) {
                    headers.put(header.getKey(), header.getValue() != null ? header.getValue() : "");
                }
            }
        }

        // Добавляем Content-Type для запросов с телом
        if (request.getBody() != null && !request.getBody().trim().isEmpty()) {
            if (!headers.containsKey("Content-Type")) {
                headers.put("Content-Type", "application/json");
            }
        }

        return headers;
    }

    private String getRequestBody(Request request) {
        String method = request.getMethod().getId().toUpperCase();
        if ("GET".equals(method) || "DELETE".equals(method) || "HEAD".equals(method)) {
            return null;
        }
        return request.getBody();
    }

    // Вспомогательные методы для обновления статуса
    private void updateExecutionTotalTests(UUID executionId, int totalTests) {
        TestExecution execution = dataManager.load(TestExecution.class).id(executionId).one();
        execution.setTotalTests(totalTests);
        dataManager.save(execution);
    }

    private void updateExecutionProgress(UUID executionId, int passed, int failed) {
        TestExecution execution = dataManager.load(TestExecution.class).id(executionId).one();
        execution.setPassedTests(passed);
        execution.setFailedTests(failed);
        dataManager.save(execution);
    }

    private void updateExecutionFinalStatus(UUID executionId, String status, int passed, int failed, long executionTime) {
        TestExecution execution = dataManager.load(TestExecution.class).id(executionId).one();
        execution.setStatus(status);
        execution.setPassedTests(passed);
        execution.setFailedTests(failed);
        execution.setExecutionTime(executionTime);
        dataManager.save(execution);
    }

    private void updateExecutionNoTests(UUID executionId, long executionTime) {
        TestExecution execution = dataManager.load(TestExecution.class).id(executionId).one();
        execution.setStatus("NO_TESTS");
        execution.setExecutionTime(executionTime);
        dataManager.save(execution);
    }

    private void updateExecutionError(UUID executionId, String errorMessage, long executionTime) {
        TestExecution execution = dataManager.load(TestExecution.class).id(executionId).one();
        execution.setStatus("ERROR");
        execution.setErrorMessage(errorMessage);
        execution.setExecutionTime(executionTime);
        execution.setPassedTests(0);
        execution.setFailedTests(execution.getTotalTests());
        dataManager.save(execution);
    }

    private String formatHeaders(Map<String, String> headers) {
        if (headers == null) return null;
        return headers.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n"));
    }
}