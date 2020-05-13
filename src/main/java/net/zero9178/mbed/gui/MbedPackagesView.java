package net.zero9178.mbed.gui;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.*;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.components.BorderLayoutPanel;
import net.zero9178.mbed.packages.MbedPackage;
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
    protected PackageView myPackageView = new PackageView();

    @NotNull
    protected TreeView myTreeView = new TreeView();

    protected static class PackageView {
        public JPanel panel;
        public JBLabel packageName;
        public JSeparator separator;
        public JBLabel currentRelease;
        public ComboBox<String> versionsAvailable;
        public JButton checkoutButton;
        public JBLabel repoLabel;
        public HyperlinkLabel repository;
    }

    public JPanel getPanel() {
        if (myPanel == null) {
            ActionToolbar actionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLWINDOW_CONTENT
                    , new DefaultActionGroup(getSideActions()), false);
            UIUtil.addBorder(actionToolbar.getComponent(), IdeBorderFactory.createBorder(JBColor.border(), SideBorder.RIGHT));
            UIUtil.addBorder(myTreeView.panel, IdeBorderFactory.createBorder(JBColor.border(), SideBorder.RIGHT));
            UIUtil.addBorder(myPackageView.panel, IdeBorderFactory.createBorder(JBColor.border(), SideBorder.LEFT));

            JBSplitter splitter = new JBSplitter();
            splitter.setFirstComponent(myTreeView.panel);
            splitter.setSecondComponent(myPackageView.panel);
            myPanel = JBUI.Panels.simplePanel(splitter).addToLeft(actionToolbar.getComponent());
        }
        return myPanel;
    }

    @NotNull
    public static MbedPackagesView getInstance(Project project) {
        return ServiceManager.getService(project, MbedPackagesView.class);
    }

    public abstract List<MbedPackage> getPackages();

    /**
     * @return List of actions to display at the very left of the tool window
     */
    protected abstract List<? extends AnAction> getSideActions();

    public abstract void refreshTree();

    public abstract void clear();
}
