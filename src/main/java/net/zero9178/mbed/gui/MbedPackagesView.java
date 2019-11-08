package net.zero9178.mbed.gui;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.SideBorder;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import net.zero9178.mbed.packages.Package;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

public abstract class MbedPackagesView {
    private BorderLayoutPanel myPanel;

    protected static class TreeView {
        public Tree tree;
        public JPanel panel;
    }

    @NotNull
    protected TreeView myTreeView = new TreeView();

    protected static class PackageView {
        public JPanel panel;
        private JLabel packageName;
    }

    @NotNull
    protected PackageView myPackageView = new PackageView();

    @NotNull
    public static MbedPackagesView getInstance(Project project) {
        return ServiceManager.getService(project, MbedPackagesView.class);
    }

    JPanel getPanel() {
        if (myPanel == null) {

            ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLWINDOW_CONTENT
                    , new DefaultActionGroup(getActions()), false);
            UIUtil.addBorder(actionToolbar.getComponent(), IdeBorderFactory.createBorder(JBColor.border(), SideBorder.RIGHT));
            UIUtil.addBorder(myTreeView.panel, IdeBorderFactory.createBorder(JBColor.border(), SideBorder.ALL));
            UIUtil.addBorder(myPackageView.panel, IdeBorderFactory.createBorder(JBColor.border(), SideBorder.ALL));

            JBSplitter splitter = new JBSplitter();
            splitter.setFirstComponent(myTreeView.panel);
            splitter.setSecondComponent(myPackageView.panel);
            myPanel = JBUI.Panels.simplePanel(splitter).addToLeft(actionToolbar.getComponent());
        }
        return myPanel;
    }

    public abstract List<Package> getPackages();

    protected abstract List<? extends AnAction> getActions();
}
