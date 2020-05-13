package net.zero9178.mbed.gui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.SpeedSearchComparator
import com.intellij.ui.TreeSpeedSearch
import com.intellij.util.ui.tree.TreeUtil
import net.zero9178.mbed.ModalTask
import net.zero9178.mbed.actions.MbedReloadChangesAction
import net.zero9178.mbed.packages.*
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
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
    private val myIsQuerying = AtomicBoolean(false)

    override fun getPackages(): List<MbedPackage> = myFlatPackageList

    init {
        myPackageView.panel.isVisible = false
        myTreeView.tree.selectionModel.selectionMode = SINGLE_TREE_SELECTION
        myTreeView.tree.addTreeSelectionListener { event ->
            if (!event.isAddedPath) {
                //Deselection
                myPackageView.panel.isVisible = false
                return@addTreeSelectionListener
            }
            //New selection
            myPackageView.panel.isVisible = true
            val lastPathComponent = event.newLeadSelectionPath.lastPathComponent as DefaultMutableTreeNode
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

            data class ComparableTriple<A : Comparable<A>, B : Comparable<B>, C : Comparable<C>>(
                val first: A,
                val second: B,
                val third: C
            ) : Comparable<ComparableTriple<A, B, C>> {
                override fun compareTo(other: ComparableTriple<A, B, C>): Int {
                    var result = first.compareTo(other.first)
                    if (result != 0) {
                        return result
                    }
                    result = second.compareTo(other.second)
                    if (result != 0) {
                        return result
                    }
                    return third.compareTo(other.third)
                }
            }
            myPackageView.versionsAvailable.model = DefaultComboBoxModel(releases.second.sortedByDescending {
                val result = "(\\d+)\\.(\\d+)\\.(\\d+)".toRegex().find(it)
                if (result == null) {
                    ComparableTriple(0, 0, 0)
                } else {
                    val (first, second, third) = result.destructured
                    ComparableTriple(first.toInt(), second.toInt(), third.toInt())
                }
            }.toTypedArray())
            if (releases.first != null) {
                myPackageView.versionsAvailable.selectedItem = releases.first
            }
        }
        TreeSpeedSearch(myTreeView.tree).comparator = SpeedSearchComparator(false)
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
    override fun refreshTree() {
        if (!myIsQuerying.compareAndSet(false, true)) {
            return
        }
        val packageFuture = queryPackages(myProject).thenAccept {
            invokeLater {
                when (it) {
                    is QueryPackageError -> myTreeView.tree.emptyText.text = it.message
                    is QueryPackageSuccess -> {
                        val selected = myTreeView.tree.lastSelectedPathComponent as? DefaultMutableTreeNode
                        val selectedPackage = selected?.userObject as? MbedPackage
                        val list = TreeUtil.collectExpandedUserObjects(myTreeView.tree).filterIsInstance<MbedPackage>()
                        myFlatPackageList.clear()
                        val root = DefaultMutableTreeNode("invisible-root")
                        insertPackages(root, listOf(it.topMbedPackage))
                        myTreeView.tree.model = DefaultTreeModel(root)
                        TreeUtil.treeNodeTraverser(root).forEach { node ->
                            if (node !is DefaultMutableTreeNode) return@forEach
                            val mbedPackage = node.userObject as? MbedPackage ?: return@forEach
                            if (list.any { it == mbedPackage }) {
                                myTreeView.tree.expandPath(TreeUtil.getPathFromRoot(node))
                            }
                            if (mbedPackage == selectedPackage) {
                                myTreeView.tree.selectionPath = TreeUtil.getPathFromRoot(node)
                            }
                        }
                    }
                }
            }
        }
        val releaseFuture = queryReleases(myProject).thenAccept {
            invokeLater {
                myReleaseMap = it
            }
        }
        CompletableFuture.allOf(packageFuture, releaseFuture).handle { _, _ ->
            myIsQuerying.set(false)
            val basePath = myProject.basePath ?: return@handle
            val file = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return@handle
            file.refresh(false, false)
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

    override fun clear() {
        myReleaseMap = emptyMap()
        myFlatPackageList.clear()
        val basePath = myProject.basePath ?: return
        val file = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return
        file.refresh(false, false)
    }
}