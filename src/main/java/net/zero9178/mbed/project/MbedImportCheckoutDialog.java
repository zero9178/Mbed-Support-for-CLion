package net.zero9178.mbed.project;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class MbedImportCheckoutDialog extends DialogWrapper {
    protected JBTextField myImportTarget;
    protected TextFieldWithBrowseButton myDirectory;
    private JPanel myPanel;

    public MbedImportCheckoutDialog(@Nullable Project project, boolean canBeParent, @NotNull IdeModalityType ideModalityType) {
        super(project, canBeParent, ideModalityType);
        init();
    }

    @Override
    protected @Nullable
    JComponent createCenterPanel() {
        return myPanel;
    }
}
