package io.genai.robots.lang.structure

import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.Sorter
import io.genai.robots.lang.RobotsFile

class RobotsStructureViewModel(file: RobotsFile) :
    StructureViewModelBase(file, RobotsStructureViewElement.forFile(file)),
    StructureViewModel.ElementInfoProvider {

    // Preserve file order (no alpha sort) — the sequence of groups is meaningful in robots.txt.
    override fun getSorters(): Array<Sorter> = Sorter.EMPTY_ARRAY

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false
    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean = false
}
