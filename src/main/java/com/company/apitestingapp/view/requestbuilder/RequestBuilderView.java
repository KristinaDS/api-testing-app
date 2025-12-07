package com.company.apitestingapp.view.requestbuilder;


import com.company.apitestingapp.entity.*;
import com.company.apitestingapp.service.apitesting.ApiTestingService;
import com.company.apitestingapp.service.apitesting.ValidationService;
import com.company.apitestingapp.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.textarea.JmixTextArea;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Route(value = "request-builder-view", layout = MainView.class)
@ViewController(id = "RequestBuilderView")
@ViewDescriptor(path = "request-builder-view.xml")
public class RequestBuilderView extends StandardView {
    @ViewComponent
    private InstanceContainer<Request> requestDc;
    @ViewComponent
    private CollectionContainer<Header> headersDc;
    @ViewComponent
    private CollectionContainer<QueryParameter> queryParamsDc;
    @ViewComponent
    private CollectionContainer<Collection> collectionsDc;

    @ViewComponent
    private ComboBox<Method> methodComboBox;
    @ViewComponent
    private ComboBox<BodyType> bodyTypeComboBox;

    @ViewComponent
    private Button sendBtn;
    @ViewComponent
    private Button addHeaderBtn;
    @ViewComponent
    private Button removeHeaderBtn;
    @ViewComponent
    private Button addParamBtn;
    @ViewComponent
    private Button removeParamBtn;

    @ViewComponent
    private DataGrid<Header> headersTable;
    @ViewComponent
    private DataGrid<QueryParameter> queryParamsTable;

    @ViewComponent
    private JmixTextArea requestBodyArea;
    @ViewComponent
    private JmixTextArea responseBodyArea;
    @ViewComponent
    private JmixTextArea responseHeadersArea;

    @ViewComponent
    private TextField urlField;
    @ViewComponent
    private TextField newParamKey;
    @ViewComponent
    private TextField newParamValue;
    @ViewComponent
    private TextField newHeaderKey;
    @ViewComponent
    private TextField newHeaderValue;

    @ViewComponent
    private Span statusLabel;
    @ViewComponent
    private Span timeLabel;
    @ViewComponent
    private Span sizeLabel;

    @Autowired
    private ApiTestingService apiTestingService;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private ValidationService validationService;


    @Subscribe
    public void onInit(final InitEvent event){
        Request request = dataManager.create(Request.class);
        requestDc.setItem(request);

        initMethodComboBox();
        initBodyTypeComboBox();
        initDataContainers();
    }

    private void initMethodComboBox(){
        methodComboBox.setItems(Method.values());
        methodComboBox.setValue(Method.GET);
        methodComboBox.setRenderer(new ComponentRenderer<>(method -> {
            Span span = new Span(method.name());
            span.addClassName("method-badge");
            span.addClassName("method-" + method.name().toLowerCase());
            span.getElement().getThemeList().add(getMethodTheme(method.name()));
            return span;
        }));

        methodComboBox.addValueChangeListener(event -> {
            if (requestDc.getItemOrNull() != null) {
                requestDc.getItem().setMethod(event.getValue());
            }
        });

        if (requestDc.getItemOrNull() != null && requestDc.getItem().getMethod() != null) {
            methodComboBox.setValue(requestDc.getItem().getMethod());
        } else {
            methodComboBox.setValue(Method.GET);
        }
    }

    private String getMethodTheme(String method){
        return switch (method) {
            case "GET" -> "badge success";
            case "POST" -> "badge primary";
            case "PUT" -> "badge contrast";
            case "PATCH" -> "badge error";
            case "DELETE" -> "badge warning";
            case "HEAD" -> "badge pill success primary";
            case "OPTIONS" -> "badge pill primary contrast";
            default -> "badge";
        };
    }

    private void initBodyTypeComboBox(){
        bodyTypeComboBox.setItems(BodyType.values());
        bodyTypeComboBox.setValue(BodyType.NONE);
        bodyTypeComboBox.addValueChangeListener(e -> {
            boolean visible = e.getValue() != BodyType.NONE;
            if (requestBodyArea != null) {
                requestBodyArea.setVisible(visible);
            }
            if (requestDc.getItemOrNull() != null) {
                requestDc.getItem().setBodyType(e.getValue());
            }
        });

        if (requestDc.getItemOrNull() != null && requestDc.getItem().getBodyType() != null) {
            bodyTypeComboBox.setValue(requestDc.getItem().getBodyType());
        } else {
            bodyTypeComboBox.setValue(BodyType.NONE);
        }
    }

    private void initDataContainers() {
        if (requestDc.getItem().getHeaders() == null) {
            requestDc.getItem().setHeaders(new ArrayList<>());
        }
        if (requestDc.getItem().getQueryParams() == null) {
            requestDc.getItem().setQueryParams(new ArrayList<>());
        }

        if (urlField != null) {
            if (requestDc.getItem().getUrl() != null) {
                urlField.setValue(requestDc.getItem().getUrl());
            }

            urlField.addValueChangeListener(event -> {
                if (requestDc.getItemOrNull() != null) {
                    requestDc.getItem().setUrl(event.getValue());
                }
            });
        }

        if (requestBodyArea != null && requestDc.getItem().getBody() != null) {
            requestBodyArea.setValue(requestDc.getItem().getBody());
        }

        if (requestBodyArea != null) {
            if (requestDc.getItem().getBody() != null) {
                requestBodyArea.setValue(requestDc.getItem().getBody());
            }

            requestBodyArea.addValueChangeListener(event -> {
                if (requestDc.getItemOrNull() != null) {
                    requestDc.getItem().setBody(event.getValue());
                }
            });
        }
    }

    @Subscribe("sendBtn")
    public void onSendBtnClick(final ClickEvent<Button> event){
        Request current = requestDc.getItem();
        if (!isValidUrl(current.getUrl())) {
            Notification.show("Неверный формат URL", 3000, Notification.Position.MIDDLE);
            return;
        }

        if (current.getBody() != null && !current.getBody().trim().isEmpty()) {
            String validationError = validationService.getValidationError(current);
            if (validationError != null) {
                Notification.show(validationError, 3000, Notification.Position.MIDDLE);
                return;
            }
        }

        executeRequest();
    }

    private void executeRequest(){
        if(!isValidUrl(urlField.getValue())){
            Notification.show("Неверный формат URL", 3000, Notification.Position.MIDDLE);
            return;
        }

        Request newRequest = dataManager.create(Request.class);
        newRequest.setUrl(urlField.getValue());
        if (requestBodyArea != null) {
            newRequest.setBody(requestBodyArea.getValue());
        } else {
            newRequest.setBody("");
        }
        newRequest.setMethod(methodComboBox.getValue());
        newRequest.setBodyType(bodyTypeComboBox.getValue());

        Request currentRequest = requestDc.getItem();
        if (currentRequest.getHeaders() != null) {
            newRequest.setHeaders(new ArrayList<>());
            for (Header header : currentRequest.getHeaders()) {
                Header newHeader = dataManager.create(Header.class);
                newHeader.setKey(header.getKey());
                newHeader.setValue(header.getValue());
                newHeader.setRequest(newRequest);
                newRequest.getHeaders().add(newHeader);
            }
        }

        if (currentRequest.getQueryParams() != null) {
            newRequest.setQueryParams(new ArrayList<>());
            for (QueryParameter param : currentRequest.getQueryParams()) {
                QueryParameter newParam = dataManager.create(QueryParameter.class);
                newParam.setKey(param.getKey());
                newParam.setValue(param.getValue());
                newParam.setRequest(newRequest);
                newRequest.getQueryParams().add(newParam);
            }
        }
        sendBtn.setEnabled(false);
        sendBtn.setText("Отправка...");

        try {
            RequestExecution execution = apiTestingService.executionRequest(newRequest);
            updateResponseUI(execution);
        } catch (Exception e) {
            Notification.show("Ошибка при выполнении запроса: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        } finally {
            sendBtn.setEnabled(true);
            sendBtn.setText("Отправить");
        }
    }

    private boolean isValidUrl(String url){
        if(url == null || url.trim().isEmpty()){
            return false;
        }
        try {
            new URL(url).toURI();
            return true;
        } catch (Exception e){
            return false;
        }
    }

    private void updateResponseUI(RequestExecution execution){
        int statusCode = execution.getStatusCode();

        // Используем ваш enum для получения описания
        String statusText;
        String statusClass;

        try {
            StatusCode status = StatusCode.fromCode(statusCode);
            if (status != null) {
                statusText = status.getCode() + " " + status.getReason();
                statusClass = getStatusClass(status);
            } else {
                statusText = statusCode + " Неизвестный статус";
                statusClass = "status-info";
            }
        } catch (Exception e) {
            statusText = statusCode + " Ошибка";
            statusClass = "status-error";
        }

        statusLabel.setText(statusText);

        // Удаляем все возможные классы статусов
        statusLabel.removeClassNames("status-success", "status-warning", "status-error", "status-info");

        // Добавляем нужный класс
        statusLabel.addClassName(statusClass);

        timeLabel.setText(execution.getResponseTimeMs() != null ?
                execution.getResponseTimeMs() + " мс" : "-");
        sizeLabel.setText(execution.getSizeBytes() != null ?
                execution.getSizeBytes() + " байт" : "-");

        if (responseHeadersArea != null) {
            String headersText = execution.getResponseHeaders() != null ?
                    execution.getResponseHeaders() : "";
            if (execution.getErrorMessage() != null && !execution.getErrorMessage().isEmpty()) {
                headersText += "\n\nОшибка: " + execution.getErrorMessage();
            }
            responseHeadersArea.setValue(headersText);
        }

        String responseBody = execution.getResponseBody() != null ?
                execution.getResponseBody() :
                (execution.getErrorMessage() != null ? execution.getErrorMessage() : "");

        updateResponseBodyWithHighlighting(responseBody);
    }

    private String getStatusClass(StatusCode status) {
        if (status == null) return "status-info";

        if (status.is1xxInformational()) {
            return "status-info";
        } else if (status.is2xxSuccessful()) {
            return "status-success";
        } else if (status.is3xxRedirection()) {
            return "status-warning";
        } else if (status.is4xxClientError()) {
            return "status-error";
        } else if (status.is5xxServerError()) {
            return "status-error";
        } else {
            return "status-info";
        }
    }

    private void updateResponseBodyWithHighlighting(String responseBody) {
        if (responseBodyArea != null) {
            if (responseBody != null && !responseBody.trim().isEmpty()) {
                if (isJson(responseBody)) {
                    String formattedJson = formatJsonForDisplay(responseBody);
                    responseBodyArea.setValue(formattedJson);
                } else if (isXml(responseBody)) {
                    String formattedXml = formatXmlForDisplay(responseBody);
                    responseBodyArea.setValue(formattedXml);
                } else {
                    responseBodyArea.setValue(responseBody);
                }
            } else {
                responseBodyArea.setValue("Нет данных для отображения");
            }
        }
    }

    private boolean isJson(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        String trimmed = text.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    private boolean isXml(String text) {
        if (text == null) return false;
        String trimmed = text.trim();
        return trimmed.startsWith("<?xml") || (trimmed.startsWith("<") && trimmed.endsWith(">"));
    }

    private String formatXmlForDisplay(String xml) {
        try {
            return xml.replace("><", ">\n<")
                    .replace("><", ">\n<")
                    .replace("><", ">\n<");
        } catch (Exception e) {
            return xml;
        }
    }

    private String formatJsonForDisplay(String json) {
        try {
            return json.replace("{", "{\n  ")
                    .replace("}", "\n}")
                    .replace(",", ",\n  ")
                    .replace("[", "[\n  ")
                    .replace("]", "\n]");
        } catch (Exception e) {
            return json;
        }
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br>")
                .replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;")
                .replace("  ", "&nbsp;&nbsp;");
    }

    @Subscribe("saveRequestBtn")
    public void onSaveRequestBtnClick(final ClickEvent<Button> event){
        Request request = requestDc.getItem();
        if (request.getUrl() == null || request.getUrl().trim().isEmpty()) {
            Notification.show("Введите URL запроса");
            return;
        }

        if (request.getMethod() == null) {
            Notification.show("Выберите метод запроса");
            return;
        }

        showSaveRequestDialog();
    }

    private void showSaveRequestDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Сохранение запроса в коллекцию");
        dialog.setWidth("500px");

        TextField requestNameField = new TextField("Название запроса");
        requestNameField.setWidth("100%");
        requestNameField.setRequired(true);
        requestNameField.setPlaceholder("Введите название запроса...");

        ComboBox<Collection> collectionComboBox = new ComboBox<>("Коллекция");
        collectionComboBox.setWidth("100%");
        collectionComboBox.setRequired(true);
        collectionComboBox.setPlaceholder("Выберите коллекцию...");

        List<Collection> collections = dataManager.load(Collection.class)
                .query("select c from Collection c order by c.name")
                .list();
        collectionComboBox.setItems(collections);
        collectionComboBox.setItemLabelGenerator(Collection::getName);

        Button createCollectionBtn = new Button("Создать новую коллекцию", e -> showCreateCollectionDialog(collectionComboBox));
        createCollectionBtn.getStyle().set("margin-top", "10px");

        FormLayout formLayout = new FormLayout();
        formLayout.add(requestNameField, collectionComboBox, createCollectionBtn);

        HorizontalLayout buttonsLayout = getHorizontalLayout(requestNameField, collectionComboBox, dialog);

        VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonsLayout);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        dialog.add(dialogLayout);
        dialog.open();
    }

    private HorizontalLayout getHorizontalLayout(TextField requestNameField, ComboBox<Collection> collectionComboBox, Dialog dialog) {
        Button saveButton = new Button("Сохранить", saveEvent -> {
            if (requestNameField.getValue() == null || requestNameField.getValue().trim().isEmpty()) {
                Notification.show("Введите название запроса");
                return;
            }

            if (collectionComboBox.getValue() == null) {
                Notification.show("Выберите коллекцию");
                return;
            }

            saveRequestToCollection(requestNameField.getValue().trim(), collectionComboBox.getValue());
            dialog.close();
        });
        saveButton.addThemeName("primary");

        Button cancelButton = new Button("Отмена", cancelEvent -> dialog.close());

        HorizontalLayout buttonsLayout = new HorizontalLayout(saveButton, cancelButton);
        buttonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        return buttonsLayout;
    }

    private void showCreateCollectionDialog(ComboBox<Collection> collectionComboBox) {
        Dialog createCollectionDialog = new Dialog();
        createCollectionDialog.setHeaderTitle("Создание новой коллекции");
        createCollectionDialog.setWidth("400px");

        TextField collectionNameField = new TextField("Название коллекции");
        collectionNameField.setWidth("100%");
        collectionNameField.setRequired(true);
        collectionNameField.setPlaceholder("Введите название коллекции...");

        TextField collectionDescriptionField = new TextField("Описание");
        collectionDescriptionField.setWidth("100%");
        collectionDescriptionField.setPlaceholder("Введите описание коллекции...");

        FormLayout formLayout = new FormLayout();
        formLayout.add(collectionNameField, collectionDescriptionField);

        Button createButton = new Button("Создать", createEvent -> {
            if (collectionNameField.getValue() == null || collectionNameField.getValue().trim().isEmpty()) {
                Notification.show("Введите название коллекции");
                return;
            }

            Collection newCollection = dataManager.create(Collection.class);
            newCollection.setName(collectionNameField.getValue().trim());
            newCollection.setDescription(collectionDescriptionField.getValue());

            try {
                Collection savedCollection = dataManager.save(newCollection);

                List<Collection> updatedCollections = dataManager.load(Collection.class)
                        .query("select c from Collection c order by c.name")
                        .list();
                collectionComboBox.setItems(updatedCollections);
                collectionComboBox.setValue(savedCollection);

                createCollectionDialog.close();
                Notification.show("Коллекция '" + savedCollection.getName() + "' создана");
            } catch (Exception e) {
                Notification.show("Ошибка при создании коллекции: " + e.getMessage());
            }
        });
        createButton.addThemeName("primary");

        Button cancelButton = new Button("Отмена", cancelEvent -> createCollectionDialog.close());

        HorizontalLayout buttonsLayout = new HorizontalLayout(createButton, cancelButton);
        buttonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        VerticalLayout dialogLayout = new VerticalLayout(formLayout, buttonsLayout);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(true);

        createCollectionDialog.add(dialogLayout);
        createCollectionDialog.open();
    }

    private void saveRequestToCollection(String requestName, Collection collection) {
        Request request = requestDc.getItem();
        request.setName(requestName);
        request.setCollection(collection);

        try {
            Request savedRequest = dataManager.save(request);
            if (headersDc.getItemOrNull() != null && !headersDc.getItems().isEmpty()) {
                for (Header header : headersDc.getItems()) {
                    header.setRequest(savedRequest);
                    dataManager.save(header);
                }
            }
            if (queryParamsDc.getItemOrNull() != null && !queryParamsDc.getItems().isEmpty()) {
                for (QueryParameter param : queryParamsDc.getItems()) {
                    param.setRequest(savedRequest);
                    dataManager.save(param);
                }
            }

            Notification.show("Запрос '" + savedRequest.getName() + "' сохранен в коллекцию '" +
                    savedRequest.getCollection().getName() + "'");
        } catch (Exception e) {
            Notification.show("Ошибка сохранения: " + e.getMessage());
        }
    }

    @Subscribe("clearBtn")
    public void onClearBtnClick(final ClickEvent<Button> event){
        Request newRequest = dataManager.create(Request.class);
        requestDc.setItem(newRequest);

        if (responseBodyArea != null) {
            responseBodyArea.setValue("");
        }
        if (responseHeadersArea != null) {
            responseHeadersArea.setValue("");
        }
        if (requestBodyArea != null) {
            requestBodyArea.setValue("");
        }

        if (queryParamsDc != null && queryParamsDc.getItemOrNull() != null) {
            queryParamsDc.getMutableItems().clear();
        }

        if (headersDc != null && headersDc.getItemOrNull() != null) {
            headersDc.getMutableItems().clear();
        }

        statusLabel.setText("Ожидание запроса...");
        statusLabel.removeClassNames("status-success", "status-error");
        timeLabel.setText("-");
        sizeLabel.setText("-");
    }

    @Subscribe("addHeaderBtn")
    public void onAddHeaderClick(final ClickEvent<Button> event) {
        String key = newHeaderKey.getValue();
        String value = newHeaderValue.getValue();

        if (key == null || key.trim().isEmpty()) {
            Notification.show("Введите ключ");
            return;
        }

        Header header = dataManager.create(Header.class);
        header.setRequest(requestDc.getItem());
        header.setKey(key.trim());
        header.setValue(value != null ? value.trim() : "");

        if (headersDc.getItemOrNull() == null) {
            requestDc.getItem().setHeaders(new ArrayList<>());
        }
        headersDc.getMutableItems().add(header);

        newHeaderKey.setValue("");
        newParamValue.setValue("");
    }

    @Subscribe("removeHeaderBtn")
    public void onRemoveHeaderClick(final ClickEvent<Button> event) {
        Header selected = headersTable.getSingleSelectedItem();
        if (selected != null) {
            headersDc.getMutableItems().remove(selected);
        }
    }

    @Subscribe("addParamBtn")
    public void onAddParamClick(final ClickEvent<Button> event) {
        String key = newParamKey.getValue();
        String value = newParamValue.getValue();

        if (key == null || key.trim().isEmpty()) {
            Notification.show("Введите ключ");
            return;
        }

        QueryParameter param = dataManager.create(QueryParameter.class);
        param.setRequest(requestDc.getItem());
        param.setKey(key.trim());
        param.setValue(value != null ? value.trim() : "");

        if (queryParamsDc.getItemOrNull() == null) {
            requestDc.getItem().setQueryParams(new ArrayList<>());
        }
        queryParamsDc.getMutableItems().add(param);

        newParamKey.setValue("");
        newParamValue.setValue("");
    }

    @Subscribe("removeParamBtn")
    public void onRemoveParamClick(final ClickEvent<Button> event) {
        QueryParameter selected = queryParamsTable.getSingleSelectedItem();
        if (selected != null) {
            queryParamsDc.getMutableItems().remove(selected);
        }
    }

    @Subscribe("copyResponseBtn")
    public void onCopyResponseClick(final ClickEvent<Button> event) {
        RequestExecution lastExecution = apiTestingService.executionRequest(requestDc.getItem());
        if (lastExecution != null) {
            String textToCopy = lastExecution.getResponseBody() != null ?
                    lastExecution.getResponseBody() :
                    lastExecution.getErrorMessage();

            if (textToCopy != null) {
                UI.getCurrent().getElement().executeJs(
                        "navigator.clipboard.writeText($0)", textToCopy
                );
                Notification.show("Ответ скопирован в буфер обмена");
            }
        }
    }


    @Subscribe("saveResponseBtn")
    public void onSaveResponseClick(final ClickEvent<Button> event) {
        //RequestExecution lastExecution = apiTestingService.executionRequest(requestDc.getItem());
        //if (lastExecution != null) {
            try {
                String statusText = statusLabel.getText();
                String timeText = timeLabel.getText();
                String sizeText = sizeLabel.getText();
                String responseBody = responseBodyArea.getValue();
                String responseHeaders = responseHeadersArea.getValue();

                int statusCode = 0;
                try {
                    String statusPart = statusText.split(" ")[0];
                    statusCode = Integer.parseInt(statusPart);
                } catch (Exception e) {
                    // ignore
                }

                String htmlReport = generateHtmlReportFromUI(statusCode, timeText, sizeText, responseBody, responseHeaders);

                UI.getCurrent().getElement().executeJs(
                        "var link = document.createElement('a');" +
                                "link.href = 'data:text/html;charset=utf-8,' + encodeURIComponent($0);" +
                                "link.download = 'api-response.html';" +
                                "link.click();", htmlReport
                );

                Notification.show("Отчет сохранен как HTML");
            } catch (Exception e) {
                Notification.show("Ошибка при сохранении отчета: " + e.getMessage());
            }
        //}
    }

    @Install(to = "headersTable.create", subject = "newEntitySupplier")
    private Header headersTableCreateNewEntitySupplier() {
        Header header = dataManager.create(Header.class);
        header.setRequest(requestDc.getItem());
        return header;
    }

    @Install(to = "queryParamsTable.create", subject = "newEntitySupplier")
    private QueryParameter queryParamsTableCreateNewEntitySupplier() {
        QueryParameter param = dataManager.create(QueryParameter.class);
        param.setRequest(requestDc.getItem());
        return param;
    }

    private String generateHtmlReport(RequestExecution execution) {
        String body = execution.getResponseBody() != null ?
                execution.getResponseBody() :
                execution.getErrorMessage();

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>API Response Report</title>\n" +
                "    <style>\n" +
                "        body { font-family: Arial, sans-serif; margin: 20px; }\n" +
                "        .status { padding: 5px 10px; border-radius: 3px; color: white; }\n" +
                "        .status-success { background-color: #28a745; }\n" +
                "        .status-error { background-color: #dc3545; }\n" +
                "        pre { background-color: #f8f9fa; padding: 15px; border-radius: 5px; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>API Response Report</h1>\n" +
                "    <p><strong>Status:</strong> <span class=\"status " +
                (execution.getIsSuccess() ? "status-success" : "status-error") + "\">" +
                execution.getStatusCode() + "</span></p>\n" +
                "    <p><strong>Time:</strong> " + execution.getResponseTimeMs() + " ms</p>\n" +
                "    <p><strong>Size:</strong> " + execution.getSizeBytes() + " bytes</p>\n" +
                "    <h2>Response Body:</h2>\n" +
                "    <pre>" + escapeHtml(body) + "</pre>\n" +
                "    <h2>Response Headers:</h2>\n" +
                "    <pre>" + escapeHtml(execution.getResponseHeaders()) + "</pre>\n" +
                "</body>\n" +
                "</html>";
    }

    private String generateHtmlReportFromUI(int statusCode, String time, String size, String body, String headers) {
        boolean isSuccess = statusCode >= 200 && statusCode < 300;

        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>API Response Report</title>\n" +
                "    <style>\n" +
                "        body { font-family: Arial, sans-serif; margin: 20px; }\n" +
                "        .status { padding: 5px 10px; border-radius: 3px; color: white; }\n" +
                "        .status-success { background-color: #28a745; }\n" +
                "        .status-error { background-color: #dc3545; }\n" +
                "        pre { background-color: #f8f9fa; padding: 15px; border-radius: 5px; white-space: pre-wrap; word-wrap: break-word; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>API Response Report</h1>\n" +
                "    <p><strong>Status:</strong> <span class=\"status " +
                (isSuccess ? "status-success" : "status-error") + "\">" +
                statusCode + "</span></p>\n" +
                "    <p><strong>Time:</strong> " + time + "</p>\n" +
                "    <p><strong>Size:</strong> " + size + "</p>\n" +
                "    <h2>Response Body:</h2>\n" +
                "    <pre>" + escapeHtml(body != null ? body : "Нет данных") + "</pre>\n" +
                "    <h2>Response Headers:</h2>\n" +
                "    <pre>" + escapeHtml(headers != null ? headers : "Нет данных") + "</pre>\n" +
                "</body>\n" +
                "</html>";
    }

}