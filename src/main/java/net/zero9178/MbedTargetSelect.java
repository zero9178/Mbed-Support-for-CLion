/*
 * Created by JFormDesigner on Wed May 29 15:08:53 CEST 2019
 */

package net.zero9178;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Schülerlizenz 2019/20
 */
public abstract class MbedTargetSelect extends DialogWrapper {

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel m_panel;
    protected ComboBox<String> myTargets;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    protected MbedTargetSelect(@Nullable Project project, boolean canBeParent, @NotNull IdeModalityType ideModalityType) {
        super(project, canBeParent, ideModalityType);
        initComponents();
        init();
    }

    @Override
    protected @Nullable
    JComponent createCenterPanel() {
        return m_panel;
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        m_panel = new JPanel();
        JBLabel m_label = new JBLabel();
        myTargets = new ComboBox<>();
        CellConstraints cc = new CellConstraints();

        //======== m_panel ========
        {
            m_panel.setLayout(new FormLayout(
                    ColumnSpec.decodeSpecs("158dlu"),
                    new RowSpec[]{
                            FormFactory.DEFAULT_ROWSPEC,
                            FormFactory.LINE_GAP_ROWSPEC,
                            FormFactory.DEFAULT_ROWSPEC
                    }));

            //---- m_label ----
            m_label.setText("Target");
            m_panel.add(m_label, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));
            m_panel.add(myTargets, cc.xy(1, 3));
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }


}
