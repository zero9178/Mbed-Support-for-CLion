package net.zero9178.mbed.gui

import com.intellij.openapi.project.Project
import net.zero9178.mbed.packages.Package
import net.zero9178.mbed.packages.QueryError
import net.zero9178.mbed.packages.QuerySuccess
import net.zero9178.mbed.packages.queryPackages
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class MbedPackagesViewImpl(private val myProject: Project) : MbedPackagesView() {
    init {
        refreshTree()
    }

    private fun refreshTree() {
        when (val result = queryPackages(myProject)) {
            is QueryError -> myTree.emptyText.text = result.message
            is QuerySuccess -> {
                val root = DefaultMutableTreeNode("invisible-root")
                insertPackages(root, result.packages)
                myTree.model = DefaultTreeModel(root)
            }
        }
    }

    private fun insertPackages(root: DefaultMutableTreeNode, packages: List<Package>) {
        for (i in packages) {
            val new = DefaultMutableTreeNode(i)
            root.add(new)
            insertPackages(new, i.dependencies)
        }
    }
}