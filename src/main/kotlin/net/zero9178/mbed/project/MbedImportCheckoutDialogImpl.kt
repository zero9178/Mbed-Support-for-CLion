package net.zero9178.mbed.project

import com.intellij.dvcs.repo.ClonePathProvider
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.util.io.exists
import net.zero9178.mbed.state.MbedState
import java.io.File
import java.nio.file.Paths
import javax.swing.event.DocumentEvent
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.DocumentFilter

class MbedImportCheckoutDialogImpl(
    project: Project,
    canBeParent: Boolean = false,
    ideModalityType: IdeModalityType = IdeModalityType.PROJECT
) : MbedImportCheckoutDialog(project, canBeParent, ideModalityType) {

    init {
        title = "Mbed import"
        (myImportTarget.document as AbstractDocument).documentFilter = object : DocumentFilter() {
            override fun insertString(fb: FilterBypass?, offset: Int, string: String?, attr: AttributeSet?) {
                if (string?.isBlank() == false) {
                    super.insertString(fb, offset, string, attr)
                }
            }
        }
        myDirectory.addBrowseFolderListener(
            TextBrowseFolderListener(
                FileChooserDescriptor(
                    false,
                    true,
                    false,
                    false,
                    false,
                    false
                )
            )
        )
        myDirectory.text = ProjectUtil.getBaseDir() + File.separatorChar
        myImportTarget.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                val parent = if (myDirectory.text.last() == File.separatorChar) {
                    Paths.get(myDirectory.text)
                } else {
                    Paths.get(myDirectory.text).parent
                }
                if (myImportTarget.text.isBlank()) {
                    myDirectory.text = parent.toString() + File.separatorChar
                } else {
                    myDirectory.text =
                        parent.resolve(ClonePathProvider.relativeDirectoryPathForVcsUrl(project, myImportTarget.text))
                            .toString()
                }
            }
        })
    }

    override fun doValidate(): ValidationInfo? {
        if (myImportTarget.text.isBlank()) {
            return ValidationInfo("No import target specified", myImportTarget)
        }
        if (Paths.get(myDirectory.text).exists()) {
            return ValidationInfo("Path already exists", myDirectory)
        }
        if (MbedState.getInstance().cliPath.isBlank()) {
            return ValidationInfo("mbed-cli is not valid")
        }
        return null
    }

    fun getImportTarget(): String = myImportTarget.text

    fun getDirectory(): String = myDirectory.text
}