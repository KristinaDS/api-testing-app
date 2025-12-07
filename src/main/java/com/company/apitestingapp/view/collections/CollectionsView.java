package com.company.apitestingapp.view.collections;


import com.company.apitestingapp.entity.*;
import com.company.apitestingapp.entity.Collection;
import com.company.apitestingapp.service.apitesting.ImportExportService;
import com.company.apitestingapp.service.apitesting.TestExecutionService;
import com.company.apitestingapp.view.main.MainView;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.upload.SucceededEvent;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.selection.SelectionEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.*;
import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.Dialogs;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.action.DialogAction;
import io.jmix.flowui.app.inputdialog.DialogActions;
import io.jmix.flowui.app.inputdialog.DialogOutcome;
import io.jmix.flowui.app.inputdialog.InputParameter;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

@Route(value = "collections-view", layout = MainView.class)
@ViewController(id = "CollectionsView")
@ViewDescriptor(path = "collections-view.xml")
public class CollectionsView extends StandardView {
    @Autowired
    private ImportExportService importExportService;
    @Autowired
    private Notifications notifications;
    @Autowired
    private Dialogs dialogs;
    @Autowired
    private DataManager dataManager;
    @Autowired
    private SystemAuthenticator systemAuthenticator;
    @Autowired
    private TestExecutionService testExecutionService;

    @ViewComponent
    private Upload importFileUpload;
    @ViewComponent
    private DataGrid<Collection> collectionsTable;
    @ViewComponent
    private DataGrid<CollectionFolder> foldersTable;
    @ViewComponent
    private DataGrid<Request> requestsTable;

    @ViewComponent
    private CollectionLoader<Collection> collectionsDl;
    @ViewComponent
    private CollectionLoader<CollectionFolder> foldersDl;
    @ViewComponent
    private CollectionLoader<Request> requestsDl;
    @ViewComponent
    private InstanceContainer<Collection> selectedCollectionDc;
    @ViewComponent
    private InstanceContainer<CollectionFolder> selectedFolderDc;

    @ViewComponent
    private Button createCollectionBtn, createCollectionBtn2;
    @ViewComponent
    private Button deleteCollectionBtn, runCollectionBtn, exportCollectionBtn;
    @ViewComponent
    private Button addFolderBtn, addSubFolderBtn, deleteFolderBtn;

    @ViewComponent
    private Button addRequestBtn, deleteRequestBtn;
//    @ViewComponent
//    private TextField searchField;

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        collectionsDl.load();
        updateButtonsState();
    }

    @Subscribe("collectionsTable")
    public void onCollectionsTableSelection(final SelectionEvent<DataGrid<Collection>, Collection> event) {
        Collection selected = collectionsTable.getSingleSelectedItem();
        selectedCollectionDc.setItem(selected);

        requestsDl.setParameter("collection", null);
        requestsDl.setParameter("folder", null);

        if (selected != null) {
            foldersDl.setParameter("collection", selected);
            foldersDl.load();

            requestsDl.setParameter("collection", selected);
            requestsDl.load();
        } else {
            foldersDl.getContainer().getMutableItems().clear();
            requestsDl.getContainer().getMutableItems().clear();
            selectedCollectionDc.setItem(null);
        }
        selectedFolderDc.setItem(null);
        foldersTable.getSelectionModel().deselectAll();
        requestsTable.getSelectionModel().deselectAll();
        updateButtonsState();
    }

    @Subscribe("foldersTable")
    public void onFoldersTableSelection(final SelectionEvent<DataGrid<CollectionFolder>, CollectionFolder> event) {
        CollectionFolder selected = foldersTable.getSingleSelectedItem();
        selectedFolderDc.setItem(selected);
        requestsDl.setParameter("collection", null);
        requestsDl.setParameter("folder", null);

        if (selected != null) {
            requestsDl.setParameter("folder", selected);
            requestsDl.load();
        } else {
            Collection collection = selectedCollectionDc.getItemOrNull();
            if (collection != null) {
                requestsDl.setParameter("collection", collection);
                requestsDl.load();
            } else {
                requestsDl.getContainer().getMutableItems().clear();
            }
        }
        requestsTable.getSelectionModel().deselectAll();
        updateButtonsState();
    }

    @Subscribe("requestsTable")
    public void onRequestsTableSelection(final SelectionEvent<DataGrid<Request>, Request> event) {
        updateButtonsState();
    }

    @Subscribe("createCollectionBtn")
    public void onCreateCollectionBtnClick(final ClickEvent<Button> event) {
        dialogs.createInputDialog(this)
                .withHeader("Создание коллекции")
                .withParameters(
                        InputParameter.stringParameter("name")
                                .withLabel("Название коллекции")
                                .withDefaultValue("Новая коллекция"),
                        InputParameter.stringParameter("description")
                                .withLabel("Описание")
                                .withDefaultValue("")
                )
                .withActions(DialogActions.OK_CANCEL)
                .withCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(DialogOutcome.OK)) {
                        String name = closeEvent.getValue("name");
                        String description = closeEvent.getValue("description");

                        Collection newCollection = dataManager.create(Collection.class);
                        newCollection.setName(name);
                        newCollection.setDescription(description);

                        dataManager.save(newCollection);
                        collectionsDl.load();

                        notifications.create("Коллекция создана")
                                .withType(Notifications.Type.SUCCESS)
                                .show();
                    }
                })
                .open();
    }

    @Subscribe("deleteCollectionBtn")
    public void onDeleteCollectionBtnClick(final ClickEvent<Button> event) {
        Collection selected = collectionsTable.getSingleSelectedItem();

        if (selected != null) {
            dialogs.createOptionDialog()
                    .withHeader("Подтверждение удаления")
                    .withText("Вы уверены, что хотите удалить коллекцию \"" + selected.getName() + "\"?")
                    .withActions(
                            new DialogAction(DialogAction.Type.YES) {
                                @Override
                                public void actionPerform(com.vaadin.flow.component.Component component) {
                                    try {
                                        // Сначала удаляем все связанные данные
                                        deleteCollectionWithDependencies(selected);

                                        collectionsDl.load();
                                        foldersDl.getContainer().getMutableItems().clear();
                                        requestsDl.getContainer().getMutableItems().clear();
                                        selectedCollectionDc.setItem(null);
                                        selectedFolderDc.setItem(null);
                                        foldersTable.getSelectionModel().deselectAll();
                                        requestsTable.getSelectionModel().deselectAll();

                                        notifications.create("Коллекция удалена")
                                                .withType(Notifications.Type.SUCCESS)
                                                .show();
                                    } catch (Exception e) {
                                        notifications.create("Ошибка при удалении коллекции: " + e.getMessage())
                                                .withType(Notifications.Type.ERROR)
                                                .show();
                                    }
                                }
                            },
                            new DialogAction(DialogAction.Type.NO)
                    )
                    .open();
        }
    }

    @Subscribe("addFolderBtn")
    public void onAddFolderBtnClick(final ClickEvent<Button> event) {
        Collection selectedCollection = selectedCollectionDc.getItemOrNull();
        if (selectedCollection == null) {
            selectedCollection = collectionsTable.getSingleSelectedItem();
        }
        if (selectedCollection != null) {
            createNewFolder(selectedCollection, null);
        } else {
            notifications.create("Выберите коллекцию")
                    .withType(Notifications.Type.WARNING)
                    .show();
        }
    }

    @Subscribe("addSubFolderBtn")
    public void onAddSubFolderBtnClick(final ClickEvent<Button> event) {
        CollectionFolder parentFolder = selectedFolderDc.getItemOrNull();
        if (parentFolder == null) {
            parentFolder = foldersTable.getSingleSelectedItem();
        }
        if (parentFolder != null) {
            createNewFolder(parentFolder.getCollection(), parentFolder);
        } else {
            notifications.create("Выберите папку для добавления подпапки")
                    .withType(Notifications.Type.WARNING)
                    .show();
        }
    }

    @Subscribe("deleteFolderBtn")
    public void onDeleteFolderBtnClick(final ClickEvent<Button> event) {
        CollectionFolder selected = foldersTable.getSingleSelectedItem();

        if (selected != null) {
            dialogs.createOptionDialog()
                    .withHeader("Подтверждение удаления")
                    .withText("Вы уверены, что хотите удалить папку \"" + selected.getName() + "\"?")
                    .withActions(
                            new DialogAction(DialogAction.Type.YES) {
                                @Override
                                public void actionPerform(com.vaadin.flow.component.Component component) {
                                    try {
                                        deleteFolderWithDependencies(selected);

                                        Collection collection = getSelectedCollection();
                                        if (collection != null) {
                                            foldersDl.setParameter("collection", collection);
                                            foldersDl.load();
                                        } else {
                                            foldersDl.getContainer().getMutableItems().clear();
                                        }
                                        requestsDl.getContainer().getMutableItems().clear();
                                        selectedFolderDc.setItem(null);
                                        requestsTable.getSelectionModel().deselectAll();

                                        notifications.create("Папка удалена")
                                                .withType(Notifications.Type.SUCCESS)
                                                .show();
                                    } catch (Exception e) {
                                        notifications.create("Ошибка при удалении папки: " + e.getMessage())
                                                .withType(Notifications.Type.ERROR)
                                                .show();
                                    }
                                }
                            },
                            new DialogAction(DialogAction.Type.NO)
                    )
                    .open();
        }
    }

    private void deleteFolderWithDependencies(CollectionFolder folder) {
        List<Request> folderRequests = dataManager.load(Request.class)
                .query("select r from Request r where r.folder = :folder")
                .parameter("folder", folder)
                .list();

        for (Request request : folderRequests) {
            deleteRelatedTestExecutionResults(request);
        }
        for (Request request : folderRequests) {
            dataManager.remove(request);
        }

        deleteSubfoldersRecursively(folder);
        dataManager.remove(folder);
    }

    private void deleteSubfoldersRecursively(CollectionFolder parentFolder) {
        List<CollectionFolder> subfolders = dataManager.load(CollectionFolder.class)
                .query("select f from CollectionFolder f where f.parentFolder = :parent")
                .parameter("parent", parentFolder)
                .list();

        for (CollectionFolder subfolder : subfolders) {
            deleteFolderWithDependencies(subfolder);
        }
    }

    private void createNewFolder(Collection collection, CollectionFolder parentFolder) {
        dialogs.createInputDialog(this)
                .withHeader(parentFolder == null ? "Новая папка" : "Новая подпапка")
                .withParameters(
                        InputParameter.stringParameter("name")
                                .withLabel("Название папки")
                                .withDefaultValue("Новая папка"),
                        InputParameter.stringParameter("description")
                                .withLabel("Описание")
                                .withDefaultValue("")
                )
                .withActions(DialogActions.OK_CANCEL)
                .withCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(DialogOutcome.OK)) {
                        String name = closeEvent.getValue("name");
                        String description = closeEvent.getValue("description");

                        CollectionFolder newFolder = dataManager.create(CollectionFolder.class);
                        newFolder.setName(name);
                        newFolder.setDescription(description);
                        newFolder.setCollection(collection);
                        newFolder.setParentFolder(parentFolder);

                        dataManager.save(newFolder);
                        foldersDl.load();

                        notifications.create("Папка создана")
                                .withType(Notifications.Type.SUCCESS)
                                .show();
                    }
                })
                .open();
    }

    @Subscribe("addRequestBtn")
    public void onAddRequestBtnClick(final ClickEvent<Button> event) {
        Collection collection = selectedCollectionDc.getItemOrNull();
        if (collection == null) {
            collection = collectionsTable.getSingleSelectedItem();
        }

        CollectionFolder folder = selectedFolderDc.getItemOrNull();
        if (folder == null) {
            folder = foldersTable.getSingleSelectedItem();
        }

        if (collection != null) {
            createNewRequest(collection, folder);
        } else {
            notifications.create("Выберите коллекцию для добавления запроса")
                    .withType(Notifications.Type.WARNING)
                    .show();
        }
    }

//    @Subscribe("editRequestBtn")
//    public void onEditRequestBtnClick(final ClickEvent<Button> event) {
//        Request selected = requestsTable.getSingleSelectedItem();
//        if (selected != null) {
//            Map<String, String> params = Collections.singletonMap("requestId", selected.getId().toString());
//            QueryParameters queryParameters = QueryParameters.simple(params);
//            UI.getCurrent().navigate("request-builder-view", queryParameters);
//        } else {
//            notifications.create("Выберите запрос для редактирования")
//                    .withType(Notifications.Type.WARNING)
//                    .show();
//        }
//    }

    @Subscribe("deleteRequestBtn")
    public void onDeleteRequestBtnClick(final ClickEvent<Button> event) {
        Request selected = requestsTable.getSingleSelectedItem();
        if (selected != null) {
            dialogs.createOptionDialog()
                    .withHeader("Подтверждение удаления")
                    .withText("Вы уверены, что хотите удалить запрос \"" + selected.getName() + "\"?")
                    .withActions(
                            new DialogAction(DialogAction.Type.YES) {
                                @Override
                                public void actionPerform(com.vaadin.flow.component.Component component) {
                                    try {
                                        // Сначала удаляем связанные TestExecutionResult
                                        deleteRelatedTestExecutionResults(selected);

                                        // Затем удаляем сам запрос
                                        dataManager.remove(selected);
                                        requestsDl.load();
                                        requestsTable.getSelectionModel().deselectAll();

                                        notifications.create("Запрос удален")
                                                .withType(Notifications.Type.SUCCESS)
                                                .show();
                                    } catch (Exception e) {
                                        notifications.create("Ошибка при удалении запроса: " + e.getMessage())
                                                .withType(Notifications.Type.ERROR)
                                                .show();
                                    }
                                }
                            },
                            new DialogAction(DialogAction.Type.NO)
                    )
                    .open();
        }
    }

    private void deleteRelatedTestExecutionResults(Request request) {
        List<TestExecutionResult> relatedResults = dataManager.load(TestExecutionResult.class)
                .query("select r from TestExecutionResult r where r.request = :request")
                .parameter("request", request)
                .list();

        for (TestExecutionResult result : relatedResults) {
            dataManager.remove(result);
        }
    }

    private void createNewRequest(Collection collection, CollectionFolder folder) {
        dialogs.createInputDialog(this)
                .withHeader("Новый запрос")
                .withParameters(
                        InputParameter.stringParameter("name")
                                .withLabel("Название запроса")
                                .withDefaultValue("Новый запрос"),
                        InputParameter.stringParameter("url")
                                .withLabel("URL")
                                .withDefaultValue("https://api.example.com/endpoint"),
                        InputParameter.stringParameter("method")
                                .withLabel("Метод")
                                .withDefaultValue("GET")
                )
                .withActions(DialogActions.OK_CANCEL)
                .withCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(DialogOutcome.OK)) {
                        String name = closeEvent.getValue("name");
                        String url = closeEvent.getValue("url");
                        String methodStr = closeEvent.getValue("method");
                        Method method = Method.valueOf(methodStr.toUpperCase());

                        Request newRequest = dataManager.create(Request.class);
                        newRequest.setName(name);
                        newRequest.setUrl(url);
                        newRequest.setMethod(method);
                        newRequest.setBodyType(BodyType.NONE);
                        newRequest.setCollection(collection);
                        newRequest.setFolder(folder);

                        dataManager.save(newRequest);
                        requestsDl.load();

                        notifications.create("Запрос создан")
                                .withType(Notifications.Type.SUCCESS)
                                .show();
                    }
                })
                .open();
    }

    @Subscribe("runCollectionBtn")
    public void onRunCollectionBtnClick(final ClickEvent<Button> event) {
        Collection selected = selectedCollectionDc.getItemOrNull();
        if (selected == null) {
            selected = collectionsTable.getSingleSelectedItem();
        }
        if (selected != null) {
            showTestRunnerDialog(selected);
//            System.out.println("Запуск коллекции: " + selected.getName() + " ID: " + selected.getId());
//            try {
//                String url = "test-runner-view?collectionId=" + selected.getId().toString();
//                System.out.println("URL навигации: " + url);
//                Map<String, String> params = Collections.singletonMap("collectionId", selected.getId().toString());
//                QueryParameters queryParameters = QueryParameters.simple(params);
//                UI.getCurrent().navigate("test-runner-view", queryParameters);
//                UI.getCurrent().getPage().setLocation(url);
//            } catch (Exception e) {
//                System.out.println("Ошибка навигации: " + e.getMessage());
//                notifications.create("Ошибка перехода: " + e.getMessage())
//                        .withType(Notifications.Type.ERROR)
//                        .show();
//            }
        } else {
            notifications.create("Выберите коллекцию для запуска")
                    .withType(Notifications.Type.WARNING)
                    .show();
        }
    }

    private void showTestRunnerDialog(Collection collection) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Запуск тестов: " + collection.getName());
        dialog.setWidth("800px");
        dialog.setHeight("600px");
        dialog.setModal(true);
        dialog.setDraggable(true);
        dialog.setResizable(true);

        VerticalLayout dialogLayout = createTestRunnerDialogContent(collection, dialog);
        dialog.add(dialogLayout);

        dialog.open();
    }

    private VerticalLayout createTestRunnerDialogContent(Collection collection, Dialog dialog) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(true);
        layout.setWidthFull();

        VerticalLayout statusLayout = new VerticalLayout();
        statusLayout.setVisible(false);
        statusLayout.setWidthFull();
        statusLayout.addClassName("status-section");

        H4 statusHeader = new H4("Выполнение тестов...");
        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setWidthFull();

        statusLayout.add(statusHeader, progressBar);

        Button runBtn = new Button("Запустить тесты");
        runBtn.addThemeNames("primary", "success");

        Button closeBtn = new Button("Закрыть", e -> dialog.close());
        closeBtn.addThemeNames("secondary");

        VerticalLayout resultsLayout = new VerticalLayout();
        resultsLayout.setWidthFull();
        resultsLayout.addClassName("results-section");

        H4 resultsHeader = new H4("Результаты выполнения");

        Grid<TestExecution> executionsGrid = createExecutionsGrid();
        executionsGrid.setWidthFull();
        executionsGrid.setHeight("300px");

        VerticalLayout detailsLayout = new VerticalLayout();
        detailsLayout.setVisible(false);
        detailsLayout.setWidthFull();
        detailsLayout.addClassName("details-section");

        H5 detailsHeader = new H5("Детали выполнения");
        Grid<TestExecutionResult> resultsGrid = createResultsGrid();
        resultsGrid.setWidthFull();
        resultsGrid.setHeight("200px");

        detailsLayout.add(detailsHeader, resultsGrid);
        resultsLayout.add(resultsHeader, executionsGrid, detailsLayout);

        loadExecutionsHistory(collection, executionsGrid, resultsGrid, detailsLayout);

        runBtn.addClickListener(e -> runTestsInDialog(collection, statusLayout, runBtn, executionsGrid, resultsGrid, detailsLayout));

        HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.setWidthFull();
        buttonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        buttonsLayout.add(runBtn, closeBtn);

        layout.add(statusLayout, buttonsLayout, resultsLayout);

        return layout;
    }

    private Grid<TestExecution> createExecutionsGrid() {
        Grid<TestExecution> grid = new Grid<>(TestExecution.class, false);

        grid.addColumn(TestExecution::getName)
                .setHeader("Название запуска")
                .setFlexGrow(1);

        grid.addColumn(TestExecution::getStatus)
                .setHeader("Статус")
                .setWidth("120px");

        grid.addColumn(TestExecution::getPassedTests)
                .setHeader("Успешно")
                .setWidth("100px");

        grid.addColumn(TestExecution::getFailedTests)
                .setHeader("Ошибки")
                .setWidth("100px");

        grid.addColumn(TestExecution::getExecutionTime)
                .setHeader("Время (мс)")
                .setWidth("120px");

        grid.addColumn(TestExecution::getCreateTs)
                .setHeader("Дата запуска")
                .setWidth("200px");

        grid.addSelectionListener(event -> {
            event.getFirstSelectedItem().ifPresent(execution -> {
                loadExecutionDetails(execution, grid.getDataProvider());
            });
        });

        return grid;
    }

    private Grid<TestExecutionResult> createResultsGrid() {
        Grid<TestExecutionResult> grid = new Grid<>(TestExecutionResult.class, false);

        grid.addColumn(TestExecutionResult::getRequest)
                .setHeader("Запрос")
                .setFlexGrow(1);

        grid.addColumn(TestExecutionResult::getStatus)
                .setHeader("Статус")
                .setWidth("100px");

        grid.addColumn(TestExecutionResult::getResponseStatus)
                .setHeader("HTTP Статус")
                .setWidth("120px");

        grid.addColumn(TestExecutionResult::getExecutionTime)
                .setHeader("Время (мс)")
                .setWidth("120px");

        grid.addColumn(TestExecutionResult::getErrorMessage)
                .setHeader("Ошибка")
                .setFlexGrow(2);

        return grid;
    }

    private void loadExecutionsHistory(Collection collection, Grid<TestExecution> executionsGrid,
                                       Grid<TestExecutionResult> resultsGrid, VerticalLayout detailsLayout) {
        List<TestExecution> executions = dataManager.load(TestExecution.class)
                .query("select te from TestExecution te where te.collection.id = :collectionId order by te.createTs desc")
                .parameter("collectionId", collection.getId())
                .list();

        executionsGrid.setItems(executions);

        detailsLayout.setVisible(false);
    }

    private void loadExecutionDetails(TestExecution execution, DataProvider<TestExecution, ?> dataProvider) {
        dataProvider.refreshAll();
    }

    private void runTestsInDialog(Collection collection, VerticalLayout statusLayout, Button runBtn, Grid<TestExecution> executionsGrid, Grid<TestExecutionResult> resultsGrid,
                                  VerticalLayout detailsLayout) {
        statusLayout.setVisible(true);
        runBtn.setEnabled(false);

        UI currentUi = UI.getCurrent();

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                TestExecution currentExecution = systemAuthenticator.withSystem(() ->
                        testExecutionService.runTestSuite(collection)
                );

                currentUi.access(() -> {
                    statusLayout.setVisible(false);
                    runBtn.setEnabled(true);

                    if ("PASSED".equals(currentExecution.getStatus())) {
                        notifications.create("Тестирование завершено успешно! Успешно: " +
                                        currentExecution.getPassedTests() + "/" + currentExecution.getTotalTests())
                                .withType(Notifications.Type.SUCCESS)
                                .withPosition(Notification.Position.BOTTOM_END)
                                .withDuration(3000)
                                .show();
                    } else if ("NO_TESTS".equals(currentExecution.getStatus())) {
                        notifications.create("В коллекции нет тестов для выполнения")
                                .withType(Notifications.Type.WARNING)
                                .withPosition(Notification.Position.BOTTOM_END)
                                .withDuration(3000)
                                .show();
                    } else {
                        notifications.create("Тестирование завершено с ошибками! Успешно: " +
                                        currentExecution.getPassedTests() + "/" + currentExecution.getTotalTests())
                                .withType(Notifications.Type.ERROR)
                                .withPosition(Notification.Position.BOTTOM_END)
                                .withDuration(3000)
                                .show();
                    }
                    loadExecutionsHistory(collection, executionsGrid, resultsGrid, detailsLayout);
                });

            } catch (Exception e) {
                currentUi.access(() -> {
                    statusLayout.setVisible(false);
                    runBtn.setEnabled(true);
                    notifications.create("Ошибка при выполнении тестов: " + e.getMessage())
                            .withType(Notifications.Type.ERROR)
                            .withPosition(Notification.Position.BOTTOM_END)
                            .withDuration(3000)
                            .show();
                });
            }
        });
    }

    private void updateButtonsState() {
        final Collection selectedCollection = getSelectedCollection();
        final CollectionFolder selectedFolder = getSelectedFolder();
        final Request selectedRequest = getSelectedRequest();

        if (selectedCollection != null && selectedCollectionDc.getItemOrNull() != selectedCollection) {
            selectedCollectionDc.setItem(selectedCollection);
        }
        if (selectedFolder != null && selectedFolderDc.getItemOrNull() != selectedFolder) {
            selectedFolderDc.setItem(selectedFolder);
        }

        // Кнопки коллекции
        deleteCollectionBtn.setEnabled(selectedCollection != null);
        runCollectionBtn.setEnabled(selectedCollection != null);
        exportCollectionBtn.setEnabled(selectedCollection != null);
        addFolderBtn.setEnabled(selectedCollection != null);

        // Кнопки папки
        addSubFolderBtn.setEnabled(selectedFolder != null);
        deleteFolderBtn.setEnabled(selectedFolder != null);

        // Кнопки запроса
        addRequestBtn.setEnabled(selectedCollection != null);
        deleteRequestBtn.setEnabled(selectedRequest != null);
    }

    // Методы импорта/экспорта
    @Subscribe("importFileUpload")
    public void onImportFileUploadSucceed(SucceededEvent event) {
        try {
            Upload uploadField = event.getSource();
            Object receiver = uploadField.getReceiver();

            if (receiver instanceof MemoryBuffer buffer) {
                InputStream inputStream = buffer.getInputStream();
                System.out.println("InputStream: " + (inputStream != null ? "не null" : "NULL!"));

                if (inputStream == null) {
                    notifications.create("Ошибка: файл не был загружен (inputStream is null)")
                            .withType(Notifications.Type.ERROR)
                            .show();
                    return;
                }

                String jsonContent;
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    int lineCount = 0;

                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                        lineCount++;
                    }
                    jsonContent = sb.toString().trim();

                } catch (Exception e) {
                    System.out.println("Ошибка чтения файла: " + e.getMessage());
                    throw e;
                }

                if (jsonContent.isEmpty()) {
                    System.out.println("Файл пустой после чтения");
                    notifications.create("Файл пустой")
                            .withType(Notifications.Type.WARNING)
                            .show();
                    return;
                }

                boolean isValid = importExportService.validateImportJson(jsonContent);
                System.out.println("Результат валидации: " + isValid);

                if (!isValid) {
                    System.out.println("Валидация не пройдена, но продолжаем импорт для теста...");
                }

                try {
                    List<Collection> importedCollections = importExportService.importAllCollectionsFromJson(jsonContent);
                    System.out.println("Успешно импортировано: " + importedCollections.size() + " коллекций");

                    collectionsDl.load();

                    notifications.create("Коллекции импортированы: " + importedCollections.size())
                            .withType(Notifications.Type.SUCCESS)
                            .show();

                } catch (Exception importException) {
                    System.out.println("Ошибка при импорте: " + importException.getMessage());
                    importException.printStackTrace();
                    notifications.create("Ошибка импорта данных: " + importException.getMessage())
                            .withType(Notifications.Type.ERROR)
                            .show();
                }

            } else {
                System.out.println("Неподдерживаемый тип receiver: " + (receiver != null ? receiver.getClass().getName() : "null"));
                notifications.create("Ошибка импорта: Неподдерживаемый тип буфера загрузки")
                        .withType(Notifications.Type.ERROR)
                        .show();
            }

        } catch (Exception e) {
            System.out.println("Критическая ошибка в обработчике: " + e.getMessage());
            e.printStackTrace();
            notifications.create("Ошибка обработки файла: " + e.getMessage())
                    .withType(Notifications.Type.ERROR)
                    .show();
        }
    }

    @Subscribe("exportCollectionBtn")
    public void onExportCollectionBtnClick(final ClickEvent<Button> event) {
        Collection selected = selectedCollectionDc.getItemOrNull();
        if (selected == null) {
            selected = collectionsTable.getSingleSelectedItem();
        }
        if (selected != null) {
            try {
                String jsonContent = importExportService.exportCollectionToJson(selected.getId());

                // Создаем StreamResource
                StreamResource resource = new StreamResource(
                        selected.getName() + ".json",
                        () -> new ByteArrayInputStream(jsonContent.getBytes(StandardCharsets.UTF_8))
                );
                resource.setContentType("application/json");

                // Регистрируем ресурс и получаем URL
                StreamRegistration registration = VaadinSession.getCurrent()
                        .getResourceRegistry()
                        .registerResource(resource);

                try {
                    String resourceUrl = registration.getResourceUri().toString();

                    UI.getCurrent().getPage().executeJs(
                            "const a = document.createElement('a'); " +
                                    "a.href = $0; " +
                                    "a.download = $1; " +
                                    "a.style.display = 'none'; " +
                                    "document.body.appendChild(a); " +
                                    "a.click(); " +
                                    "document.body.removeChild(a);",
                            resourceUrl,
                            selected.getName() + ".json"
                    );

                    notifications.create("Коллекция экспортирована")
                            .withType(Notifications.Type.SUCCESS)
                            .show();

                } finally {
                    // Опционально: можно отрегистрировать ресурс после скачивания
                    // registration.unregister();
                }

            } catch (Exception e) {
                notifications.create("Ошибка экспорта: " + e.getMessage())
                        .withType(Notifications.Type.ERROR)
                        .show();
            }
        } else {
            notifications.create("Выберите коллекцию для экспорта")
                    .withType(Notifications.Type.WARNING)
                    .show();
        }
    }

    private Collection getSelectedCollection() {
        Collection collection = selectedCollectionDc.getItemOrNull();
        if (collection == null) {
            collection = collectionsTable.getSingleSelectedItem();
        }
        return collection;
    }

    private CollectionFolder getSelectedFolder() {
        CollectionFolder folder = selectedFolderDc.getItemOrNull();
        if (folder == null) {
            folder = foldersTable.getSingleSelectedItem();
        }
        return folder;
    }

    private Request getSelectedRequest() {
        return requestsTable.getSingleSelectedItem();
    }

    private void deleteCollectionWithDependencies(Collection collection) {
        // 1. Удаляем TestExecutionResult, связанные с TestExecution этой коллекции
        deleteTestExecutionResultsForCollection(collection);

        // 2. Удаляем TestExecution для этой коллекции
        deleteTestExecutionsForCollection(collection);

        // 3. Удаляем TestExecutionResult, напрямую связанные с Request этой коллекции
        deleteTestExecutionResultsForCollectionRequests(collection);

        // 4. Удаляем все Request коллекции
        deleteCollectionRequests(collection);

        // 5. Удаляем все CollectionFolder коллекции
        deleteCollectionFolders(collection);

        // 6. Удаляем саму коллекцию
        dataManager.remove(collection);
    }

    private void deleteTestExecutionResultsForCollection(Collection collection) {
        List<TestExecutionResult> results = dataManager.load(TestExecutionResult.class)
                .query("SELECT r FROM TestExecutionResult r " +
                        "WHERE r.testExecution IN (" +
                        "    SELECT e FROM TestExecution e WHERE e.collection = :collection" +
                        ")")
                .parameter("collection", collection)
                .list();

        for (TestExecutionResult result : results) {
            dataManager.remove(result);
        }
    }

    private void deleteTestExecutionsForCollection(Collection collection) {
        List<TestExecution> executions = dataManager.load(TestExecution.class)
                .query("SELECT e FROM TestExecution e WHERE e.collection = :collection")
                .parameter("collection", collection)
                .list();

        for (TestExecution execution : executions) {
            dataManager.remove(execution);
        }
    }

    private void deleteTestExecutionResultsForCollectionRequests(Collection collection) {
        List<TestExecutionResult> results = dataManager.load(TestExecutionResult.class)
                .query("SELECT r FROM TestExecutionResult r " +
                        "WHERE r.request IN (" +
                        "    SELECT req FROM Request req WHERE req.collection = :collection" +
                        ")")
                .parameter("collection", collection)
                .list();

        for (TestExecutionResult result : results) {
            dataManager.remove(result);
        }
    }

    private void deleteCollectionRequests(Collection collection) {
        List<Request> requests = dataManager.load(Request.class)
                .query("SELECT r FROM Request r WHERE r.collection = :collection")
                .parameter("collection", collection)
                .list();

        for (Request request : requests) {
            dataManager.remove(request);
        }
    }

    private void deleteCollectionFolders(Collection collection) {
        List<CollectionFolder> folders = dataManager.load(CollectionFolder.class)
                .query("SELECT f FROM CollectionFolder f WHERE f.collection = :collection")
                .parameter("collection", collection)
                .list();

        for (CollectionFolder folder : folders) {
            dataManager.remove(folder);
        }
    }

}