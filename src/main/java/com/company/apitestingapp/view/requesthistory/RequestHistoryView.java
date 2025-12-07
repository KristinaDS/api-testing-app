package com.company.apitestingapp.view.requesthistory;


import com.company.apitestingapp.entity.BodyType;
import com.company.apitestingapp.entity.Method;
import com.company.apitestingapp.entity.Request;
import com.company.apitestingapp.entity.RequestExecution;
import com.company.apitestingapp.service.apitesting.ApiTestingService;
import com.company.apitestingapp.service.apitesting.HtmlReportService;
import com.company.apitestingapp.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "request-history-view", layout = MainView.class)
@ViewController(id = "RequestHistoryView")
@ViewDescriptor(path = "request-history-view.xml")
public class RequestHistoryView extends StandardView {
    @ViewComponent
    private DataGrid<RequestExecution> historyGrid;
    @ViewComponent
    private TextField searchField;
    @ViewComponent
    private ComboBox<String> methodFilter;
    @ViewComponent
    private ComboBox<String> statusFilter;
    @ViewComponent
    private DatePicker dateFromFilter;
    @ViewComponent
    private DatePicker dateToFilter;
    @ViewComponent
    private CollectionContainer<RequestExecution> requestExecutionsDc;
    @ViewComponent
    private CollectionLoader<RequestExecution> requestExecutionsDl;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private ApiTestingService apiTestingService;
    @Autowired
    private HtmlReportService htmlReportService;


    @Subscribe
    public void onInit(final InitEvent event) {
        initFilters();
        setupGridRenderers();
        setupGridDoubleClick();
        loadHistory();
    }

    private void initFilters(){
        methodFilter.setItems("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");
        methodFilter.setPlaceholder("Все методы");

        statusFilter.setItems("Успешные", "Ошибки", "Все");
        statusFilter.setValue("Все");

        dateFromFilter.setValue(LocalDate.now().minusDays(7));
        dateToFilter.setValue(LocalDate.now());

        methodFilter.addValueChangeListener(e -> applyFilters());
        statusFilter.addValueChangeListener(e -> applyFilters());
        dateFromFilter.addValueChangeListener(e -> applyFilters());
        dateToFilter.addValueChangeListener(e -> applyFilters());
        searchField.addValueChangeListener(e -> applyFilters());
    }

    private void setupGridRenderers(){
        List<DataGrid.Column<RequestExecution>> columns = historyGrid.getColumns();

        columns.get(0).setRenderer(new ComponentRenderer<>(exec -> {
            String method = exec.getRequest().getMethod() != null ? exec.getRequest().getMethod().name() : "N/A";
            Span span = new Span(method);
            span.getElement().getThemeList().add("badge " + getMethodTheme(method));
            return span;
        }));

        columns.get(1).setRenderer(new ComponentRenderer<>(exec -> {
            String url = exec.getRequest().getUrl() != null ? exec.getRequest().getUrl() : "N/A";
            return new Span(getShortUrl(url));
        }));

        columns.get(2).setRenderer(new ComponentRenderer<>(this::createStatusBadge));

        columns.get(3).setRenderer(new ComponentRenderer<>(exec ->
                new Span(exec.getResponseTimeMs() + " мс")));

        columns.get(4).setRenderer(new ComponentRenderer<>(exec ->
                new Span(exec.getExecutedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")))));
    }

    private Component createStatusBadge(RequestExecution execution) {
        Span span = new Span();
        if (Boolean.TRUE.equals(execution.getIsSuccess())) {
            span.setText("✓ " + execution.getStatusCode());
            span.getElement().getThemeList().add("badge success");
        } else {
            span.setText("✗ " + execution.getStatusCode());
            span.getElement().getThemeList().add("badge error");
        }
        return span;
    }

    private void setupGridDoubleClick(){
        historyGrid.addItemDoubleClickListener(event -> {
            RequestExecution execution = event.getItem();
            openExecutionDetails(execution);
        });
    }

    private void loadHistory(){
        applyFilters();
    }

    private void applyFilters(){
        String methodStr = methodFilter.getValue();
        String status = statusFilter.getValue();
        LocalDate from = dateFromFilter.getValue();
        LocalDate to = dateToFilter.getValue();
        String search = searchField.getValue();

        System.out.println("DEBUG: Filters - Method: " + methodStr +
                ", Status: " + status +
                ", From: " + from +
                ", To: " + to +
                ", Search: " + search);

        // Загружаем все записи один раз
        List<RequestExecution> allExecutions = dataManager.load(RequestExecution.class)
                .query("select e from RequestExecution e order by e.executedAt desc")
                .list();

        // Фильтруем на клиенте
        List<RequestExecution> filtered = allExecutions.stream()
                .filter(exec -> filterByMethod(exec, methodStr))
                .filter(exec -> filterByStatus(exec, status))
                .filter(exec -> filterByDate(exec, from, to))
                .filter(exec -> filterBySearch(exec, search))
                .collect(Collectors.toList());

        System.out.println("DEBUG: Filtered records: " + filtered.size());

        requestExecutionsDc.setItems(filtered);
    }

    private boolean filterByMethod(RequestExecution exec, String methodStr) {
        if (methodStr == null || methodStr.isEmpty()) {
            return true;
        }
        return exec.getRequest().getMethod() != null &&
                exec.getRequest().getMethod().name().equals(methodStr);
    }

    private boolean filterByStatus(RequestExecution exec, String status) {
        if (status == null || status.isEmpty() || "Все".equals(status)) {
            return true;
        }
        if ("Успешные".equals(status)) {
            return Boolean.TRUE.equals(exec.getIsSuccess());
        }
        if ("Ошибки".equals(status)) {
            return Boolean.FALSE.equals(exec.getIsSuccess());
        }
        return true;
    }

    private boolean filterByDate(RequestExecution exec, LocalDate from, LocalDate to) {
        LocalDate execDate = exec.getExecutedAt().toLocalDate();

        if (from != null && execDate.isBefore(from)) {
            return false;
        }
        if (to != null && execDate.isAfter(to)) {
            return false;
        }
        return true;
    }

    private boolean filterBySearch(RequestExecution exec, String search) {
        if (search == null || search.trim().isEmpty()) {
            return true;
        }

        String searchLower = search.toLowerCase().trim();

        // Поиск по URL
        if (exec.getRequest().getUrl() != null &&
                exec.getRequest().getUrl().toLowerCase().contains(searchLower)) {
            return true;
        }

        // Поиск по методу
        if (exec.getRequest().getMethod() != null &&
                exec.getRequest().getMethod().name().toLowerCase().contains(searchLower)) {
            return true;
        }

        // Поиск по статус коду
        if (exec.getStatusCode() != null &&
                exec.getStatusCode().toString().contains(search)) {
            return true;
        }

        // Поиск по сообщению об ошибке
        if (exec.getErrorMessage() != null &&
                exec.getErrorMessage().toLowerCase().contains(searchLower)) {
            return true;
        }

        // Поиск по телу ответа
        if (exec.getResponseBody() != null &&
                exec.getResponseBody().toLowerCase().contains(searchLower)) {
            return true;
        }

        return false;
    }

    private String getMethodTheme(String method) {
        return switch (method) {
            case "GET" -> "success";
            case "POST" -> "primary";
            case "PUT" -> "contrast";
            case "DELETE" -> "error";
            case "PATCH" -> "warning";
            case "HEAD" -> "success primary";
            case "OPTIONS" -> "contrast primary";
            default -> "";
        };
    }

    private String getShortUrl(String url) {
        if (url.length() > 50) {
            return url.substring(0, 47) + "...";
        }
        return url;
    }

    private void openExecutionDetails(RequestExecution execution) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Детали выполнения запроса");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);

        String method = execution.getRequest().getMethod() != null ? execution.getRequest().getMethod().name() : "N/A";
        String url = execution.getRequest().getUrl() != null ? execution.getRequest().getUrl() : "N/A";

        HorizontalLayout infoLayout = new HorizontalLayout();
        infoLayout.add(
                createInfoBadge("Метод", method, getMethodTheme(method)),
                createInfoBadge("Статус", execution.getStatusCode() != null ? execution.getStatusCode().toString() : "N/A",
                        Boolean.TRUE.equals(execution.getIsSuccess()) ? "success" : "error"),
                createInfoBadge("Время", execution.getResponseTimeMs() + " мс", "contrast"),
                createInfoBadge("Размер", execution.getSizeBytes() + " байт", "contrast")
        );

        TextArea urlField = new TextArea("URL");
        urlField.setValue(url);
        urlField.setReadOnly(true);
        urlField.setWidthFull();

        TextArea responseBody = new TextArea("Тело ответа");
        responseBody.setValue(execution.getResponseBody() != null ? execution.getResponseBody() :
                execution.getErrorMessage() != null ? execution.getErrorMessage() : "Нет данных");
        responseBody.setReadOnly(true);
        responseBody.setWidthFull();
        responseBody.setHeight("300px");

        content.add(infoLayout, urlField, responseBody);
        dialog.add(content);

        Button closeButton = new Button("Закрыть", e -> dialog.close());
        dialog.getFooter().add(closeButton);

        dialog.open();
    }

    private Component createInfoBadge(String label, String value, String theme) {
        Span span = new Span(label + ": " + value);
        span.getElement().getThemeList().add("badge " + theme);
        span.getStyle().set("margin-right", "10px");
        return span;
    }

    @Subscribe("clearHistoryBtn")
    public void onClearHistoryButtonClick(final ClickEvent<Button> event) {
        ConfirmDialog dialog = new ConfirmDialog(
                "Очистка истории",
                "Вы уверены, что хотите удалить всю историю запросов?",
                "Да",
                this::clearHistory,
                "Нет",
                null
        );
        dialog.open();
    }

    private void clearHistory(ConfirmDialog.ConfirmEvent confirmEvent) {
        try {
            List<RequestExecution> allExecutions = dataManager.load(RequestExecution.class)
                    .all()
                    .list();

            for (RequestExecution execution : allExecutions) {
                dataManager.remove(execution);
            }

            loadHistory();
            Notification.show("История очищена. Удалено записей: " + allExecutions.size());
        } catch (Exception e) {
            Notification.show("Ошибка при очистке истории: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE);
        }
    }

    @Subscribe("refreshBtn")
    public void onRefreshButtonClick(final ClickEvent<Button> event) {
        loadHistory();
        Notification.show("История обновлена");
    }

    @Subscribe("repeatRequestBtn")
    public void onRepeatRequestClick(final ClickEvent<Button> event) {
        RequestExecution selected = historyGrid.getSingleSelectedItem();
        if (selected != null) {
            Request request = dataManager.create(Request.class);
            request.setUrl(selected.getRequest().getUrl());

            if (selected.getRequest().getMethod() != null) {
                try {
                    request.setMethod(Method.valueOf(selected.getRequest().getMethod().name()));
                } catch (IllegalArgumentException e) {
                    request.setMethod(Method.GET);
                }
            } else {
                request.setMethod(Method.GET);
            }

            request.setBody(selected.getRequest().getBody());
            request.setBodyType(BodyType.JSON);

            RequestExecution newExecution = apiTestingService.executionRequest(request);
            loadHistory();
            Notification.show("Запрос повторно выполнен");
        }
    }

    @Subscribe("exportHistoryBtn")
    public void onExportHtmlBtnClick(final ClickEvent<Button> event) {
        List<RequestExecution> allExecutions = dataManager.load(RequestExecution.class)
                .query("select e from RequestExecution e " +
                        "order by e.executedAt desc")
                .list();

        if (allExecutions.isEmpty()) {
            Notification.show("Нет данных для экспорта");
            return;
        }

        String filename = "api-test-report-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")) + ".html";

        try {
            htmlReportService.exportReportToFile(allExecutions, filename);
            Notification.show("HTML отчет экспортирован: " + filename);
        } catch (Exception e) {
            Notification.show("Ошибка экспорта: " + e.getMessage());
        }
    }
}