@file:Suppress("unused")

package com.sysu.example

import android.content.Context
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.util.containsKey
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class BaseRecyclerViewHolder(private val view: View, var data: Any? = null) : RecyclerView.ViewHolder(view) {
    private val viewCache = SparseArray<View>()

    fun getView() = view

    @Suppress("UNCHECKED_CAST")
    fun <T : View> findViewById(id: Int): T {
        val result: T
        if (!viewCache.containsKey(id)) {
            result = view.findViewById(id)
            viewCache.put(id, result)
        } else {
            result = viewCache.get(id) as T
        }
        return result
    }

    companion object {
        fun get(context: Context, parent: ViewGroup, @LayoutRes layoutId: Int) =
            BaseRecyclerViewHolder(LayoutInflater.from(context).inflate(layoutId, parent, false))
    }
}

class BaseRecyclerAdapter<T>(
    private val context: Context,
    private val dataSet: MutableList<T>, @LayoutRes private val layoutId: Int,
    private val action: AdapterAction<T>
) : RecyclerView.Adapter<BaseRecyclerViewHolder>() {

    // click

    private var itemClickListener: ItemClickListener<T>? = null
    fun setItemClickListener(itemClickListener: ItemClickListener<T>?) {
        this.itemClickListener = itemClickListener
    }

    // single add remove get update move

    fun addData(data: T, pos: Int? = null): Boolean {
        val size = dataSet.size
        val truePos = pos ?: size
        if (truePos < 0 || truePos > size) {
            return false
        }
        dataSet.add(truePos, data)
        notifyItemInserted(truePos)
        return true
    }

    fun removeData(pos: Int): T? {
        if (pos < 0 || pos >= dataSet.size) {
            return null
        }
        val old = dataSet.removeAt(pos)
        notifyItemRemoved(pos)
        return old
    }

    fun removeData(data: T): Boolean {
        val pos = dataSet.indexOf(data)
        if (pos != -1 && dataSet.remove(data)) {
            notifyItemRemoved(pos)
            return true
        }
        return false
    }

    fun getData(pos: Int): T? {
        if (pos >= 0 && pos < dataSet.size) {
            return dataSet[pos]
        }
        return null
    }

    fun setData(data: T, pos: Int): Boolean {
        if (pos >= 0 && pos < dataSet.size) {
            dataSet[pos] = data
            notifyItemChanged(pos)
            return true
        }
        return false
    }

    fun moveData(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition < 0 || toPosition < 0 || fromPosition >= dataSet.size || toPosition >= dataSet.size) {
            return false
        }
        val sourceData: T = dataSet[fromPosition]
        val increment = if (fromPosition < toPosition) 1 else -1
        var i = fromPosition
        while (i != toPosition) {
            dataSet[i] = dataSet[i + increment]
            i += increment
        }
        dataSet[toPosition] = sourceData
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    // range add remove get update

    fun addDataRanged(dataSet: List<T>, pos: Int? = null): Boolean {
        val size = dataSet.size
        val truePos = pos ?: size
        if (truePos < 0 || truePos > this.dataSet.size) {
            return false
        }
        this.dataSet.addAll(truePos, dataSet)
        notifyItemRangeInserted(truePos, size)
        return true
    }

    fun removeDataRanged(pos: Int, len: Int = -1): Boolean {
        val size = dataSet.size
        if (pos < 0 || pos >= size || len < 0 && len != -1) {
            return false
        }
        val trueLen = if (len != -1 && pos + len < size) {
            pos + len
        } else {
            size
        }
        (pos until pos + trueLen).forEach { dataSet.removeAt(it) }
        notifyItemRangeRemoved(pos, trueLen)
        return true
    }

    fun getDataRanged(pos: Int, len: Int = -1): List<T> {
        return dataSet.subList(
            pos, if (len == -1) {
                dataSet.size
            } else {
                pos + len
            }
        )
    }

    fun setDataRanged(dataSet: List<T>, pos: Int): Boolean {
        val size = this.dataSet.size
        if (pos < 0 || pos >= size) {
            return false
        }
        (pos until size).forEach { this.dataSet[it] = dataSet[it - pos] }
        notifyItemRangeChanged(
            pos, if (dataSet.size + pos > size) {
                size - pos
            } else {
                dataSet.size
            }
        )
        return true
    }

    fun filterData(action: (T) -> Boolean) {
        val result = dataSet.filter(action)
        dataSet.clear()
        dataSet.addAll(result)
        notifyDataSetChanged()
    }

    fun setDataSet(dataSet: List<T>) {
        this.dataSet.clear()
        this.dataSet.addAll(dataSet)
        notifyDataSetChanged()
    }

    fun clear() {
        if (dataSet.isEmpty()) {
            return
        }
        val itemCount = dataSet.size
        dataSet.clear()
        notifyItemRangeRemoved(0, itemCount)
    }

    val size: Int
        get() = dataSet.size

    // view / header / footer / item_empty

    private var headerView: View? = null
    private var footerView: View? = null
    private var emptyView: View? = null

    fun setHeader(headerView: View) {
        this.headerView = headerView
    }

    fun hasHeader() = headerView != null
    fun hasFooter() = footerView != null
    fun hasEmptyView() = emptyView != null

    fun setFooter(footerView: View) {
        this.footerView = footerView
    }

    fun setEmptyView(emptyView: View) {
        this.emptyView = emptyView
    }

    override fun getItemCount(): Int {
        var result = action.getItemCount() ?: dataSet.size
        if (headerView != null) {
            result++
        }
        if (footerView != null) {
            result++
        }
        if (emptyView != null && result == 0) {
            result = 1
        }
        return result
    }

    override fun getItemViewType(position: Int): Int {
        if (dataSet.isEmpty()) {
            return TYPE_EMPTY
        }
        var truePos = position
        if (headerView != null) {
            if (truePos == 0) {
                return TYPE_HEADER
            }
            truePos--
        }
        if (footerView != null && truePos == dataSet.size) {
            return TYPE_FOOTER
        }
        return action.getItemViewType(dataSet[truePos], truePos) ?: super.getItemViewType(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseRecyclerViewHolder {
        return when {
            dataSet.isEmpty() && viewType == TYPE_EMPTY ->
                emptyView?.let { BaseRecyclerViewHolder(it) } ?: BaseRecyclerViewHolder.get(context, parent, R.layout.item_empty)
            viewType == TYPE_HEADER -> BaseRecyclerViewHolder(headerView!!)
            viewType == TYPE_FOOTER -> BaseRecyclerViewHolder(footerView!!)
            else -> BaseRecyclerViewHolder.get(context, parent, layoutId)
        }
    }

    override fun onBindViewHolder(holder: BaseRecyclerViewHolder, position: Int) {
        if (dataSet.isEmpty()) {
            return
        }
        if (position == dataSet.size) {
            if (footerView == null) {
                throw IllegalArgumentException("footerView is null and position should not be greater than dataSet's size")
            }
            return
        }
        var truePos = position
        if (headerView != null) {
            if (truePos == 0)
                return
            truePos--
        }
        val data = dataSet[truePos]
        holder.data = data
        action.onBindViewHolder(holder, data, truePos)
        itemClickListener?.let {
            holder.getView().run {
                setOnClickListener { itemClickListener!!.onItemClick(holder, data, truePos) }
                setOnLongClickListener { itemClickListener!!.onItemLongClick(holder, data, truePos) }
            }
        }
    }

    private var recyclerView: RecyclerView? = null

    fun getRightPos(pos: Int): Int = if (headerView != null) pos + 1 else pos

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        val layoutManager = recyclerView.layoutManager
        if (layoutManager is GridLayoutManager) {
            layoutManager.spanSizeLookup = object : SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (headerView != null && position == 0 || footerView != null && position == dataSet.size + 1 || dataSet.size == 0) layoutManager.spanCount else 1
                }
            }
        }
    }

    override fun onViewAttachedToWindow(holder: BaseRecyclerViewHolder) {
        val layoutParams: ViewGroup.LayoutParams = holder.itemView.layoutParams
        if (layoutParams is StaggeredGridLayoutManager.LayoutParams) {
            layoutParams.isFullSpan = holder.itemViewType == TYPE_FOOTER || holder.itemViewType == TYPE_HEADER || holder.itemViewType == TYPE_EMPTY
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
    }

    // interface

    interface ItemClickListener<T> {
        fun onItemClick(holder: BaseRecyclerViewHolder, data: T, position: Int) = Unit
        fun onItemLongClick(holder: BaseRecyclerViewHolder, data: T, position: Int): Boolean = false
    }

    interface AdapterAction<T> {
        fun onBindViewHolder(holder: BaseRecyclerViewHolder, data: T, position: Int)
        fun getItemCount(): Int? = null
        fun getItemViewType(data: T, position: Int): Int? = null
        // TODO: 各种 on
    }

    // const

    companion object {
        const val TYPE_EMPTY = 1
        const val TYPE_HEADER = 2
        const val TYPE_FOOTER = 3
    }

    // 上拉刷新 / 下拉刷新
    //
}
