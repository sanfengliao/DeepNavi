package com.sysu.example.fragment

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.liang.example.map.bean.DeepNaviPoint
import com.sysu.example.KeyUrls
import com.sysu.example.R
import com.sysu.example.utils.ContextApi
import com.liang.example.map.net.EdgeApi.addEdge
import com.sysu.example.KeyUrls.ADD_EDGE
import com.sysu.example.utils.doPostMainAsync
import com.sysu.example.utils.returnToast3
import kotlinx.android.synthetic.main.fragment_add_edge.add_end_point
import kotlinx.android.synthetic.main.fragment_add_edge.add_start_point
import kotlinx.android.synthetic.main.fragment_add_edge.point_choice

open class AddEdgeDialogFragment(
    protected open var mapId: String,
    protected open var points: List<DeepNaviPoint>,
    protected open var edgeUpdater: UpdateMapEdge
) : BaseDialogFragment(R.layout.fragment_add_edge) {
    protected open var pId1: String? = null
    protected open var pId2: String? = null
    protected open var innerChanged: Boolean = false
    protected open var pointChoice: Int = 2

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        point_choice.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.point_id -> pointChoice = 0
                R.id.point_name -> pointChoice = 1
                R.id.point_index -> pointChoice = 2
            }
            innerChanged = true
            add_start_point.text = null
            add_end_point.text = null
            innerChanged = false
        }
        add_start_point.addTextChangedListener {
            pId1 = parsePointId(it?.toString()?.trim())
            if (!innerChanged) {
                edgeUpdater.update(pId1, pId2)
            }
        }
        add_end_point.addTextChangedListener {
            pId2 = parsePointId(it?.toString()?.trim())
            if (!innerChanged) {
                edgeUpdater.update(pId1, pId2)
            }
        }
        view.findViewById<Button>(R.id.ok)?.setOnClickListener ok@{
            pId1 = parsePointId(add_start_point.text?.toString()?.trim())
            pId2 = parsePointId(add_end_point.text?.toString()?.trim())
            if (pId1 == null || pId2 == null) {
                return@ok returnToast3("start point's id and end point's id should not be empty")
            }
            addEdge(ADD_EDGE, mapId, pId1!!, pId2!!) { msg, flag ->
                ContextApi.toast(msg)
                if (flag) {
                    edgeUpdater.update(pId1, pId2, 2)
                    dismiss()
                }
            }
        }
        view.findViewById<Button>(R.id.cancel)?.setOnClickListener {
            edgeUpdater.update(pId1, pId2, 1)
            dismiss()
        }
    }

    protected open fun parsePointId(text: String?): String? {
        if (text == null) {
            return null
        }
        return when (pointChoice) {
            0 -> text
            1 -> points.find { it.name == text }?.id
            else -> when (val index = text.toIntOrNull()) {
                null -> null
                else -> points[index].id
            }
        }
    }

    interface UpdateMapEdge {
        fun update(pId1: String?, pId2: String?, flag: Int = 0)
    }
}
