package net.zero9178.mbed.gui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import net.zero9178.mbed.packages.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION

class MbedPackagesViewImpl(private val myProject: Project) : MbedPackagesView() {
    private val myFlatPackageList = mutableListOf<Package>()
    private var myReleaseMap = mapOf<String, Pair<String?, List<String>>>()

    override fun getPackages(): List<Package> = myFlatPackageList

    init {
        refreshTree()
        myTreeView.tree.selectionModel.selectionMode = SINGLE_TREE_SELECTION
    }

    private fun refreshTree() {
        when (val result = queryPackages(myProject)) {
            is QueryPackageError -> myTreeView.tree.emptyText.text = result.message
            is QueryPackageSuccess -> {
                myFlatPackageList.clear()
                val root = DefaultMutableTreeNode("invisible-root")
                insertPackages(root, listOf(result.topPackage))
                myTreeView.tree.model = DefaultTreeModel(root)
            }
        }
        myReleaseMap = queryReleases(myProject)
    }

    private fun insertPackages(root: DefaultMutableTreeNode, packages: List<Package>) {
        myFlatPackageList += packages
        for (i in packages) {
            val new = DefaultMutableTreeNode(i)
            root.add(new)
            insertPackages(new, i.dependencies)
        }
    }

    override fun getActions(): MutableList<out AnAction> {
        return mutableListOf(object : AnAction(AllIcons.Actions.Refresh) {
            override fun actionPerformed(e: AnActionEvent) {
                refreshTree()
            }
        })
    }
}