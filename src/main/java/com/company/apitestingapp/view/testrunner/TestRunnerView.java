package com.company.apitestingapp.view.testrunner;


import com.company.apitestingapp.entity.Collection;
import com.company.apitestingapp.entity.TestExecution;
import com.company.apitestingapp.entity.TestExecutionResult;
import com.company.apitestingapp.service.apitesting.TestExecutionService;
import com.company.apitestingapp.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.data.selection.SelectionEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.Notifications;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

@Route(value = "test-runner-view", layout = MainView.class)
@ViewController(id = "TestRunnerView")
@ViewDescriptor(path = "test-runner-view.xml")
public class TestRunnerView extends StandardView {
    @ViewComponent
    private CollectionLoader<Collection> collectionsDl;
    @ViewComponent
    private CollectionContainer<Collection> collectionsDc;
    @ViewComponent
    private CollectionLoader<TestExecution> testExecutionsDl;
    @ViewComponent
    private CollectionLoader<TestExecutionResult> executionResultsDl;

    @ViewComponent
    private ComboBox<Collection> collectionComboBox;
    @ViewComponent
    private Button runTestsBtn;
    @ViewComponent
    private VerticalLayout executionStatusBox;
    @ViewComponent
    private ProgressBar executionProgress;
    @ViewComponent
    private VerticalLayout executionDetails;
    @ViewComponent
    private DataGrid<TestExecution> testExecutionsGrid;
    @ViewComponent
    private DataGrid<TestExecutionResult> executionResultsGrid;

    @Autowired
    private TestExecutionService testExecutionService;
    @Autowired
    private Notifications notifications;
    @Autowired
    private SystemAuthenticator systemAuthenticator;


    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        collectionsDl.load();
        testExecutionsDl.load();
        setupComboBoxes();
    }

    private void setupComboBoxes() {
        collectionComboBox.setItems(collectionsDc.getItems());
        collectionComboBox.setItemLabelGenerator(Collection::getName);

        collectionComboBox.addValueChangeListener(event -> {
            updateRunButtonState();
        });
    }

    private void updateRunButtonState() {
        runTestsBtn.setEnabled(collectionComboBox.getValue() != null);
    }

    @Subscribe("runTestsBtn")
    public void onRunTestsBtnClick(final ClickEvent<Button> event) {
        Collection collection = collectionComboBox.getValue();

        if (collection == null) {
            notifications.create("Выберите коллекцию для тестирования")
                    .withType(Notifications.Type.WARNING)
                    .show();
            return;
        }
        runTestsInBackground(collection);
    }

    private void runTestsInBackground(Collection collection) {
        showExecutionStatus(true);
        runTestsBtn.setEnabled(false);

        UI currentUi = UI.getCurrent();

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                TestExecution currentExecution = systemAuthenticator.withSystem(() -> testExecutionService.runTestSuite(collection));

                currentUi.access(() -> {
                    runTestsBtn.setEnabled(true);
                    showExecutionStatus(false);

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
                    testExecutionsDl.load();
                });

            } catch (Exception e) {
                currentUi.access(() -> {
                    runTestsBtn.setEnabled(true);
                    showExecutionStatus(false);
                    notifications.create("Ошибка при выполнении тестов: " + e.getMessage())
                            .withType(Notifications.Type.ERROR)
                            .withPosition(Notification.Position.BOTTOM_END)
                            .withDuration(3000)
                            .show();
                });
            }
        });
    }

    private void showExecutionStatus(boolean show) {
        executionStatusBox.setVisible(show);
        if (show) {
            executionProgress.setIndeterminate(true);
        }
    }

    @Subscribe("testExecutionsGrid")
    public void onTestExecutionsGridSelection(final SelectionEvent<DataGrid<TestExecution>, TestExecution> event) {
        TestExecution selected = testExecutionsGrid.getSingleSelectedItem();

        if (selected != null && selected.getId() != null) {
            executionResultsDl.setParameter("testExecutionId", selected.getId());
            executionResultsDl.load();
            executionDetails.setVisible(true);
        } else {
            executionDetails.setVisible(false);
        }
    }
}