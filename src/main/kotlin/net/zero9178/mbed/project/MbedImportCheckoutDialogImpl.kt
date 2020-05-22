package net.zero9178.mbed.project

import com.intellij.dvcs.repo.ClonePathProvider
import com.intellij.dvcs.ui.CloneDvcsValidationUtils
import com.intellij.dvcs.ui.DvcsBundle
import com.intellij.dvcs.ui.SelectChildTextFieldWithBrowseButton
import com.intellij.execution.configurations.PtyCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.ui.VcsCloneComponent
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.io.exists
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import git4idea.GitVcs
import net.zero9178.mbed.MbedNotification
import net.zero9178.mbed.MbedSyncTask
import net.zero9178.mbed.packages.changeTarget
import net.zero9178.mbed.packages.changeTargetDialog
import net.zero9178.mbed.state.MbedState
import org.zmlx.hg4idea.HgVcs
import java.io.File
import java.net.URI
import java.nio.file.Paths
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

private const val MBED_BASE_URL = "https://github.com/ARMmbed"

/**
 * GUI shown when using the mbed import functionality from the VCS menu
 */
class MbedImportCheckoutDialogImpl(private val project: Project) : VcsCloneComponent {

    private val mainPanel: JPanel
    private val urlEditor = JBTextField()
    private val directoryField = SelectChildTextFieldWithBrowseButton(
        ClonePathProvider.defaultParentDirectoryPath(project, MbedRememberedInputs.getInstance())
    )

    private lateinit var errorComponent: BorderLayoutPanel

    init {
        val fcd = FileChooserDescriptorFactory.createSingleFolderDescriptor()
        fcd.isShowFileSystemRoots = true
        fcd.isHideIgnored = false
        directoryField.addBrowseFolderListener(
            DvcsBundle.getString("clone.destination.directory.browser.title"),
            DvcsBundle.getString("clone.destination.directory.browser.description"),
            project,
            fcd
        )
        mainPanel = panel {
            row(VcsBundle.getString("vcs.common.labels.url")) { urlEditor(growX) }
            row(VcsBundle.getString("vcs.common.labels.directory")) { directoryField(growX) }
                .largeGapAfter()
            row {
                errorComponent = BorderLayoutPanel(UIUtil.DEFAULT_HGAP, 0)
                errorComponent()
            }
        }

        val insets = UIUtil.PANEL_REGULAR_INSETS
        mainPanel.border = JBEmptyBorder(insets.top / 2, insets.left, insets.bottom, insets.right)

        urlEditor.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                directoryField.trySetChildPath(
                    ClonePathProvider.relativeDirectoryPathForVcsUrl(
                        project,
                        urlEditor.text.trim()
                    )
                )
            }
        })
    }

    override fun getPreferredFocusedComponent(): JComponent = urlEditor

    override fun getView() = mainPanel

    override fun isOkEnabled() = false

    override fun doValidateAll(): List<ValidationInfo> {
        val list = ArrayList<ValidationInfo>()
        ContainerUtil.addIfNotNull(
            list,
            CloneDvcsValidationUtils.checkDirectory(directoryField.text, directoryField.textField)
        )
        val url = urlEditor.text.trim().let {
            when {
                URI(it).isAbsolute -> {
                    it
                }
                Paths.get(it).exists() -> {
                    it
                }
                else -> {
                    MBED_BASE_URL + File.pathSeparatorChar + it
                }
            }
        }
        ContainerUtil.addIfNotNull(list, CloneDvcsValidationUtils.checkRepositoryURL(urlEditor, url))
        return list
    }

    override fun dispose() {}

    override fun doClone(project: Project, listener: CheckoutProvider.Listener) {
        val directory = directoryField.text.trim()
        ProgressManager.getInstance().run(
            MbedSyncTask(
                project,
                "Importing mbed project",
                {
                    val cli = MbedState.getInstance().cliPath
                    val handler = OSProcessHandler(
                        PtyCommandLine(
                            listOf(
                                cli,
                                "import",
                                urlEditor.text,
                                directory
                            )
                        )
                    )
                    var output = ""
                    handler.addProcessListener(object : ProcessAdapter() {
                        override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                            it.text += event.text
                            output += event.text
                        }
                    })
                    handler.startNotify()
                    handler.waitFor()
                    if (handler.exitCode != 0) {
                        Notifications.Bus.notify(
                            MbedNotification.GROUP_DISPLAY_ID_INFO.createNotification(
                                "mbed failed with exit code ${handler.exitCode}\n Output: $output",
                                NotificationType.ERROR
                            )
                        )
                    }
                })
        )
        if (Paths.get(directory).resolve(".git").exists()) {
            listener.directoryCheckedOut(File(directory), GitVcs.getKey())
        } else {
            listener.directoryCheckedOut(File(directory), HgVcs.getKey())
        }
        listener.checkoutCompleted()
        val rememberedInputs = MbedRememberedInputs.getInstance()
        rememberedInputs.addUrl(urlEditor.text)
        rememberedInputs.cloneParentDir = Paths.get(directory).toAbsolutePath().parent.toString()
        val newProject = ProjectManager.getInstance().openProjects.find {
            it.basePath == directory.replace('\\', '/')
        } //finds just created project. We can't use the project variable passed from the function as its just a
        // temporary empty project for the checkout process
        if (newProject != null) {
            changeTargetDialog(newProject)?.let { changeTarget(it, newProject) }
            CMakeWorkspace.getInstance(newProject).selectProjectDir(newProject.basePath?.let { File(it) })
        }
    }
}