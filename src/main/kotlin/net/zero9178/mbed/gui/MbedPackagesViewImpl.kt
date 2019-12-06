package net.zero9178.mbed.gui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import net.zero9178.mbed.ModalTask
import net.zero9178.mbed.actions.MbedReloadChangesAction
import net.zero9178.mbed.packages.*
import java.nio.file.Paths
import javax.swing.DefaultComboBoxModel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION

/**
 * GUI for package management
 */
class MbedPackagesViewImpl(private val myProject: Project) : MbedPackagesView() {
    private val myFlatPackageList = mutableListOf<MbedPackage>()
    private var myReleaseMap = mapOf<String, Pair<String?, List<String>>>()

    override fun getPackages(): List<MbedPackage> = myFlatPackageList

    init {
        myPackageView.panel.isVisible = false
        refreshTree()
        myTreeView.tree.selectionModel.selectionMode = SINGLE_TREE_SELECTION
        myTreeView.tree.addTreeSelectionListener {
            if (it.isAddedPath) {
                //New selection
                myPackageView.panel.isVisible = true
                val lastPathComponent = it.newLeadSelectionPath.lastPathComponent as DefaultMutableTreeNode
                val mbedPackage = lastPathComponent.userObject as MbedPackage
                myPackageView.packageName.text = mbedPackage.name
                if (mbedPackage.repo != null) {
                    myPackageView.repoLabel.isVisible = true
                    myPackageView.repository.isVisible = true
                    myPackageView.repository.setHyperlinkText(mbedPackage.repo)
                    myPackageView.repository.setHyperlinkTarget(mbedPackage.repo)
                } else {
                    myPackageView.repoLabel.isVisible = false
                    myPackageView.repository.isVisible = false
                }
                val releases = myReleaseMap[mbedPackage.name] ?: return@addTreeSelectionListener
                myPackageView.currentRelease.text = releases.first ?: "<UNKNOWN>"
                myPackageView.versionsAvailable.model = DefaultComboBoxModel(releases.second.toTypedArray())
                if (releases.first != null) {
                    myPackageView.versionsAvailable.selectedItem = releases.first
                }
            } else {
                //Deselection
                myPackageView.panel.isVisible = false
            }
        }
        myPackageView.checkoutButton.addActionListener {
            val array = myTreeView.tree.selectionPath?.path?.run {
                slice(1..lastIndex)
            } ?: return@addActionListener
            val basePath = myProject.basePath ?: return@addActionListener
            var path = Paths.get(basePath).parent
            for (iter in array) {
                path = path.resolve(iter.toString())
            }
            val item = myPackageView.versionsAvailable.selectedItem ?: return@addActionListener
            var result = false
            ProgressManager.getInstance().run(ModalTask(myProject, "Updating project", {
                result = updatePackage(path.toString(), item.toString(), myProject)
            }) {
                if (result) {
                    refreshTree()
                    MbedReloadChangesAction.update(myProject)
                }
            })
        }
    }

    /**
     * Refreshes dependencies of application async
     */
    private fun refreshTree() {
        queryPackages(myProject).thenAccept {
            ApplicationManager.getApplication().invokeLater {
                when (it) {
                    is QueryPackageError -> myTreeView.tree.emptyText.text = it.message
                    is QueryPackageSuccess -> {
                        myFlatPackageList.clear()
                        val root = DefaultMutableTreeNode("invisible-root")
                        insertPackages(root, listOf(it.topMbedPackage))
                        myTreeView.tree.model = DefaultTreeModel(root)
                    }
                }
            }
        }

        queryReleases(myProject).thenAccept {
            ApplicationManager.getApplication().invokeLater {
                myReleaseMap = it
            }
        }
    }

    private fun insertPackages(root: DefaultMutableTreeNode, mbedPackages: List<MbedPackage>) {
        myFlatPackageList += mbedPackages
        for (i in mbedPackages) {
            val new = DefaultMutableTreeNode(i)
            root.add(new)
            insertPackages(new, i.dependencies)
        }
    }

    override fun getSideActions(): MutableList<out AnAction> {
        return mutableListOf(object : AnAction(AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) {
                refreshTree()
            }
        })
    }
}