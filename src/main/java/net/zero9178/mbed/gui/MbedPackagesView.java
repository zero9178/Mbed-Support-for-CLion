package net.zero9178.mbed.gui;

import com.intellij.openapi.project.Project;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class MbedPackagesView {
    protected Tree myTree;
    private JPanel myPanel;

    @NotNull
    public static MbedPackagesView getInstance(Project project) {
        return project.getComponent(MbedPackagesView.class);
    }

    JPanel getPanel() {
        return myPanel;
    }

}
