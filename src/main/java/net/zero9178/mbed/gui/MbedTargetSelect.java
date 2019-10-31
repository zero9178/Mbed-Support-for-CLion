/*
 * Created by JFormDesigner on Wed May 29 15:08:53 CEST 2019
 */

package net.zero9178.mbed.gui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class MbedTargetSelect extends DialogWrapper {

    protected ComboBox<String> myTargets;
    private JPanel myPanel;
    protected JBLabel myCurrentTarget;

    protected MbedTargetSelect(@Nullable Project project, boolean canBeParent, @NotNull IdeModalityType ideModalityType) {
        super(project, canBeParent, ideModalityType);
        init();
    }

    @Override
    protected @Nullable
    JComponent createCenterPanel() {
        return myPanel;
    }
}
