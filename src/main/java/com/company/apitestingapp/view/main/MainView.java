package com.company.apitestingapp.view.main;

import com.company.apitestingapp.view.collections.CollectionsView;
import com.company.apitestingapp.view.requestbuilder.RequestBuilderView;
import com.company.apitestingapp.view.requesthistory.RequestHistoryView;
import com.company.apitestingapp.view.testrunner.TestRunnerView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.ViewNavigators;
import io.jmix.flowui.app.main.StandardMainView;
import io.jmix.flowui.kit.component.main.ListMenu;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.flowui.view.ViewController;
import io.jmix.flowui.view.ViewDescriptor;
import io.jmix.flowui.view.navigation.ViewNavigationSupport;
import io.jmix.flowui.view.navigation.ViewNavigator;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;

@Route("")
@ViewController(id = "MainView")
@ViewDescriptor(path = "main-view.xml")
public class MainView extends StandardMainView {
    @ViewComponent
    private ListMenu menu;
    @ViewComponent
    private Button collectionsBtn;
    @ViewComponent
    private Button historyBtn;
    @ViewComponent
    private Button settingsBtn;
    @ViewComponent
    private Button helpBtn;
    @ViewComponent
    private Button quickNewRequest;
    @ViewComponent
    private Button quickImport;
    @Autowired
    private ViewNavigators viewNavigators;


    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event){

    }

    @Subscribe("collectionsBtn")
    public void setCollectionsBtnClick(final ClickEvent<Button> event){
        viewNavigators.view(this, CollectionsView.class).navigate();
    }

    @Subscribe("historyBtn")
    public void onHistoryBtnClick(final ClickEvent<Button> event){
        viewNavigators.view(this, RequestHistoryView.class).navigate();
    }

    @Subscribe("settingsBtn")
    public void onSettingsBtnClick(final ClickEvent<Button> event) {

    }

    @Subscribe("helpBtn")
    public void onHelpBtnClick(final ClickEvent<Button> event) {

    }

    @Subscribe("quickNewRequest")
    public void onQuickNewRequestClick(final ClickEvent<Button> event){
        viewNavigators.view(this, RequestBuilderView.class).navigate();
    }

    @Subscribe("quickTestRunner")
    public void onQuickTestRunClick(final ClickEvent<Button> event){
        viewNavigators.view(this, TestRunnerView.class).navigate();
    }
}
