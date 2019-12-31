@file: Suppress("LeakingThis", "SENSELESS_COMPARISON")

package com.sysu.example.block

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.core.view.updateLayoutParams
import java.util.*
import kotlin.collections.ArrayList

// TODO: 线程安全

open class BlockGroup(@LayoutRes layoutId: Int = 0) : Block(layoutId) {
    open val children: MutableList<Block> = Collections.synchronizedList(ArrayList())
    open val addBlockTasks: MutableList<Runnable> = Collections.synchronizedList(ArrayList())
    open val viewGroup: ViewGroup?
        get() = view as? ViewGroup
    private val SYNC_OBJECT = Any()
    private var constructorFlag = 1

    // init

    // 使用这个不需要依靠BlockManager，不需要自己使用 init(context)
    constructor(context: Context, @LayoutRes layoutId: Int = 0) : this(layoutId) {
        init(context)
    }

    constructor(blockManager: BlockManager, @LayoutRes layoutId: Int = 0) : this(layoutId) {
        this.context = blockManager.context
        this.inflater = blockManager.inflater
        this.provider = provider
        this.swb = blockManager.swb
        this.cwb = blockManager.cwb
        this.rxHandler = blockManager.rxHandler
        this.blockManager = blockManager
        constructorFlag = 2
    }

    // inflate

    @Suppress("UNCHECKED_CAST")
    override fun <T : View> setInflatedCallback(callback: (T) -> Unit): BlockGroup {
        afterInflateListener = Runnable { callback(view as T) }
        return this
    }

    open var inflateBlocksAsync: Boolean = false

    @CallSuper
    override fun afterInflateView() {
        children.filter { it.inflated.get() && it.view!!.parent == null }.forEach { addViewOfBlock(it) }
        addBlockTasks.forEach { it.run() }
        addBlockTasks.clear()
    }

    open fun build(parent: ViewGroup? = null) = build(parent, -1)
    override fun build(parent: ViewGroup?, index: Int?): BlockGroup = super.build(parent, index) as BlockGroup

    // refresh

    override fun refresh() = refreshGroup()
    override fun refreshGroup(): Unit = children.forEach { it.refresh() }

    // activity's on

    override fun onNewIntent(intent: Intent) = children.forEach { it.onNewIntent(intent) }
    override fun onBackPressed() = children.forEach { it.onBackPressed() }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) =
            children.forEach { it.onActivityResult(requestCode, resultCode, data) }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) =
            children.forEach { it.onRequestPermissionsResult(requestCode, permissions, grantResults) }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        children.forEach { if (it.onKeyDown(keyCode, event)) return true }
        return false
    }

    // add

    open fun generateLayoutParams(w: Int, h: Int) = ViewGroup.LayoutParams(w, h)

    private fun innerAddBlock(block: Block, index: Int = -1): BlockGroup {
        if (block in children) {
            return this
        }
        if (index == -1) {
            children.add(block)
        } else {
            children.add(index, block)
        }
        if (block.inflated.get()) {
            block.recycle()
        }
        block.init(this.swb, this.cwb, this.rxHandler, this, this.blockManager)
        block.inflateViewAsync = inflateBlocksAsync
        block.inflate(context, inflater, viewGroup)
        return this
    }

    private fun checkTask(runnable: Runnable): BlockGroup {
        if (this.inflated.get()) {
            runnable.run()
        } else {
            addBlockTasks.add(runnable)
        }
        return this
    }

    open fun addBlock(block: Block) = innerAddBlock(block)
    open fun addBlockIf(condition: Boolean, block: Block) = if (condition) addBlock(block) else this
    open fun addBlockIf(condition: () -> Boolean, block: Block) = if (condition()) addBlock(block) else this

    open fun addBlockLater(block: Block) = checkTask(Runnable { innerAddBlock(block) })
    open fun addBlockLaterIf(condition: Boolean, block: Block) = if (condition) checkTask(Runnable { innerAddBlock(block) }) else this
    open fun addBlockLaterIf(condition: () -> Boolean, block: Block) =
            if (condition()) checkTask(Runnable { innerAddBlock(block) }) else this

    open fun insertBlock(block: Block, index: Int) = innerAddBlock(block, index)
    open fun insertBlockIf(condition: Boolean, block: Block, index: Int) = if (condition) innerAddBlock(block, index) else this
    open fun insertBlockIf(condition: () -> Boolean, block: Block, index: Int) = if (condition()) innerAddBlock(block, index) else this

    open fun insertBlockLater(block: Block, index: Int) = checkTask(Runnable { innerAddBlock(block, index) })
    open fun insertBlockLaterIf(condition: Boolean, block: Block, index: Int) =
            if (condition) checkTask(Runnable { innerAddBlock(block, index) }) else this

    open fun insertBlockLaterIf(condition: () -> Boolean, block: Block, index: Int) =
            if (condition()) checkTask(Runnable { innerAddBlock(block, index) }) else this

    open fun addViewOfBlock(block: Block): BlockGroup = synchronized(SYNC_OBJECT) {
        if (viewGroup == null) {
            return this
        }
        var lastBlock: Block? = null
        for (child in children) {
            if (block == child) {
                break
            }
            if (child.inflated.get()) {
                lastBlock = child
            }
        }
        if (lastBlock == null) {
            viewGroup!!.addView(block.view)
        } else {
            viewGroup!!.addView(block.view, viewGroup!!.indexOfChild(lastBlock.view) + 1)
        }
        block.parent = viewGroup!!
        return this
    }

    // remove

    open fun removeBlock(block: Block): BlockGroup = synchronized(SYNC_OBJECT) {
        children.remove(block)
        if (block.inflated.get()) {
            viewGroup?.removeView(block.view)
        }
        return this
    }

    open fun removeBlock(index: Int): BlockGroup = synchronized(SYNC_OBJECT) {
        val block = children.removeAt(index)
        if (block.inflated.get()) {
            viewGroup?.removeView(block.view)
        }
        return this
    }

    open fun removeBlockIf(condition: Boolean, block: Block) = if (condition) removeBlock(block) else this
    open fun removeBlockIf(condition: () -> Boolean, block: Block) = if (condition()) removeBlock(block) else this

    open fun removeBlockIf(condition: Boolean, index: Int) = if (condition) removeBlock(index) else this
    open fun removeBlockIf(condition: () -> Boolean, index: Int) = if (condition()) removeBlock(index) else this

    // replace

    open fun replaceBlock(newBlock: Block, oldBlock: Block): BlockGroup = synchronized(SYNC_OBJECT) {
        if (oldBlock !in children) {
            return this
        }
        val index = children.indexOf(oldBlock)
        children.remove(oldBlock)
        children.add(index, newBlock)
        if (oldBlock.inflated.get()) {
            viewGroup?.removeView(oldBlock.view)
        }
        if (newBlock.inflated.get()) {
            newBlock.recycle()
        }
        newBlock.init(this.swb, this.cwb, this.rxHandler, this, this.blockManager)
        newBlock.inflate(context, inflater, viewGroup)
        return this
    }

    open fun replaceBlock(newBlock: Block, index: Int): BlockGroup = synchronized(SYNC_OBJECT) {
        val oldBlock = children.removeAt(index)
        children.add(index, newBlock)
        if (oldBlock.inflated.get()) {
            viewGroup?.removeView(oldBlock.view)
        }
        if (newBlock.inflated.get()) {
            newBlock.recycle()
        }
        newBlock.init(this.swb, this.cwb, this.rxHandler, this, this.blockManager)
        newBlock.inflate(context, inflater, viewGroup)
        return this
    }

    open fun replaceBlockIf(condition: Boolean, newBlock: Block, oldBlock: Block) = if (condition) replaceBlock(newBlock, oldBlock) else this
    open fun replaceBlockIf(condition: () -> Boolean, newBlock: Block, oldBlock: Block) = if (condition()) replaceBlock(newBlock, oldBlock) else this

    open fun replaceBlockIf(condition: Boolean, newBlock: Block, index: Int) = if (condition) replaceBlock(newBlock, index) else this
    open fun replaceBlockIf(condition: () -> Boolean, newBlock: Block, index: Int) = if (condition()) replaceBlock(newBlock, index) else this

    // find

    open fun getBlock(index: Int) = children[index]
    open fun getInflatedBlock(index: Int) = if (children[index].inflated.get()) children[index] else null

    open fun getBlocks() = children
    open fun getInflatedBlocks() = children.filter { it.inflated.get() }
    open fun getLeafBlocks(): List<Block> {
        val leafBlocks: MutableList<Block> = ArrayList()
        for (child in children) {
            if (child is BlockGroup) {
                leafBlocks.addAll(child.getLeafBlocks())
            } else {
                leafBlocks.add(child)
            }
        }
        return leafBlocks
    }

    open fun findBlockById(id: Int) = children.find { it.viewId == id }
    open fun findBlockByTag(tag: Any) = children.find { it.view?.tag ?: 0 == tag }

    // builder

    class Builder(private val context: Context) {  // TODO: 模仿 AnimatorSet 的 TODO
        private var blockManager: BlockManager? = null

        constructor(blockManager: BlockManager) : this(blockManager.context) {
            this.blockManager = blockManager
        }

        fun index(block: Block) {
            TODO()
        }

        fun before(block: Block) {
            TODO()
        }

        fun after(block: Block) {
            TODO()
        }

        fun build(): BlockGroup {
            TODO()
        }
    }
}

open class FragmentBlockGroup(context: Context) : BlockGroup(context), FragmentLifeCycleInter {
    open var bundle: Bundle? = null

    override fun onAttach(context: Context) {
        this.context = context
        this.children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onAttach(context) }
    }

    override fun onCreate(bundle: Bundle?) {
        this.bundle = bundle
        this.children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onCreate(bundle) }
    }

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, bundle: Bundle?): View? {
        this.bundle = bundle
        this.children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onCreateView(inflater, parent, bundle) }
        return this.view
    }

    override fun onActivityCreated(bundle: Bundle?) =
            this.children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onActivityCreated(bundle) }

    override fun onStart() = this.children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onStart() }
    override fun onResume() = this.children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onResume() }
    override fun onPause() = this.children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onPause() }
    override fun onStop() = this.children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onStop() }
    override fun onDestroyView() = this.children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onDestroyView() }
    override fun onDestroy() = this.children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onDestroy() }
    override fun onDetach() = this.children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onDetach() }
    override fun onSaveInstanceState(bundle: Bundle) =
            this.children.filterIsInstance<FragmentLifeCycleInter>().forEach { it.onSaveInstanceState(bundle) }
}

open class ActivityBlockGroup(context: Context) : BlockGroup(context), ActivityLifeCycleInter {
    open var bundle: Bundle? = null

    override fun onCreate(bundle: Bundle?) {
        this.bundle = bundle
        this.children.filterIsInstance<ActivityLifeCycleInter>().forEach { it.onCreate(bundle) }
    }

    override fun onRestart() = this.children.filterIsInstance<ActivityLifeCycleInter>().forEach { it.onRestart() }
    override fun onStart() = this.children.filterIsInstance<ActivityLifeCycleInter>().forEach { it.onStart() }
    override fun onResume() = this.children.filterIsInstance<ActivityLifeCycleInter>().forEach { it.onResume() }
    override fun onPause() = this.children.filterIsInstance<ActivityLifeCycleInter>().forEach { it.onPause() }
    override fun onStop() = this.children.filterIsInstance<ActivityLifeCycleInter>().forEach { it.onStop() }
    override fun onDestroy() = this.children.filterIsInstance<ActivityLifeCycleInter>().forEach { it.onDestroy() }
    override fun onSaveInstanceState(bundle: Bundle) =
            this.children.filterIsInstance<ActivityLifeCycleInter>().forEach { it.onSaveInstanceState(bundle) }

    override fun onRestoreInstanceState(bundle: Bundle) {
        this.bundle = bundle
        this.children.filterIsInstance<ActivityLifeCycleInter>().forEach { it.onRestoreInstanceState(bundle) }
    }
}
