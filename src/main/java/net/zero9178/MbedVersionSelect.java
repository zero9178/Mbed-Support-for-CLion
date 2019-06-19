/*
 * Created by JFormDesigner on Wed Jun 19 13:02:57 CEST 2019
 */

package net.zero9178;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.components.JBLabel;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Schülerlizenz 2019/20
 */
public abstract class MbedVersionSelect extends DialogWrapper {
    protected ComboBox<String> m_version;
    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel m_panel;
    private JBLabel m_loading;

    protected MbedVersionSelect(@Nullable Project project, boolean canBeParent, @NotNull IdeModalityType ideModalityType) {
        super(project, canBeParent, ideModalityType);
        initComponents();
        init();
    }

    protected void setLoading(boolean loading) {
        m_loading.setIcon(loading ? new AnimatedIcon.Default() : null);
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
        m_version = new ComboBox<>();
        m_loading = new JBLabel();
        CellConstraints cc = new CellConstraints();

        //======== m_panel ========
        {
            m_panel.setLayout(new FormLayout(
                    new ColumnSpec[]{
                            new ColumnSpec(Sizes.dluX(138)),
                            FormFactory.LABEL_COMPONENT_GAP_COLSPEC,
                            FormFactory.DEFAULT_COLSPEC
                    },
                    new RowSpec[]{
                            FormFactory.DEFAULT_ROWSPEC,
                            FormFactory.LINE_GAP_ROWSPEC,
                            FormFactory.DEFAULT_ROWSPEC
                    }));

            //---- m_label ----
            m_label.setText("Version");
            m_label.setHorizontalAlignment(SwingConstants.CENTER);
            m_panel.add(m_label, cc.xy(1, 1));
            m_panel.add(m_version, cc.xy(1, 3));

            //---- m_loading ----
            m_loading.setToolTipText("Querying versions...");
            m_panel.add(m_loading, cc.xy(3, 3));
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
