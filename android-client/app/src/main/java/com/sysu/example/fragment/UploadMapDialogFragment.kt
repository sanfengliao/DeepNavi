package com.sysu.example.fragment

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.sysu.example.R
import com.liang.example.map.bean.DeepNaviMap
import com.liang.example.map.bean.INVALID_FLOAT_ARRAY
import com.liang.example.map.net.MapApi.uploadMap
import com.sysu.example.KeyUrls.UPLOAD_MAP
import com.sysu.example.utils.ContextApi
import com.sysu.example.utils.returnToast2
import com.sysu.example.utils.returnToast3
import kotlinx.android.synthetic.main.fragment_upload_map.is_clock_wise
import kotlinx.android.synthetic.main.fragment_upload_map.map_actual_size_x
import kotlinx.android.synthetic.main.fragment_upload_map.map_actual_size_y
import kotlinx.android.synthetic.main.fragment_upload_map.map_actual_size_z
import kotlinx.android.synthetic.main.fragment_upload_map.map_bitmap_size_x
import kotlinx.android.synthetic.main.fragment_upload_map.map_bitmap_size_y
import kotlinx.android.synthetic.main.fragment_upload_map.map_bitmap_size_z
import kotlinx.android.synthetic.main.fragment_upload_map.map_name
import kotlinx.android.synthetic.main.fragment_upload_map.map_offset_angle
import kotlinx.android.synthetic.main.fragment_upload_map.map_rotation_x
import kotlinx.android.synthetic.main.fragment_upload_map.map_rotation_y
import kotlinx.android.synthetic.main.fragment_upload_map.map_rotation_z
import kotlinx.android.synthetic.main.fragment_upload_map.select_map_drawable
import kotlinx.android.synthetic.main.fragment_upload_map.set_origin_point

open class UploadMapDialogFragment(
    protected open var bitmapUpdater: UpdateMapBitmap,
    protected open var originPointUpdater: AddPointDialogFragment.UpdateMapPoint,
    protected open var mapInfoUpdater: UpdateMapInfo
) : BaseDialogFragment(R.layout.fragment_upload_map) {
    protected open var bitmap: Bitmap? = null
    protected open val mapInfo = DeepNaviMap()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        select_map_drawable.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), IMAGE_RESULT_CODE)
        }
        setOriginPoint()
        map_offset_angle.setText("0")
        map_bitmap_size_z.setText("0")
        map_actual_size_z.setText("0")
        view.findViewById<Button>(R.id.ok).setOnClickListener ok@{
            val bitmap = this.bitmap ?: return@ok returnToast3("map's bitmap should be select")
            mapInfo.name = map_name.text?.toString()?.trim() ?: return@ok returnToast3("map's name should not be empty")
            if (getPlanSize() || getActualSize() || getAngleRotation()) {
                return@ok
            }
            if (mapInfo.originInPlan === INVALID_FLOAT_ARRAY) {
                return@ok returnToast3("you should select the origin point")
            }
            mapInfo.isClockWise = is_clock_wise.isChecked
            mapInfo.offsetAngle = map_offset_angle.text?.toString()?.toFloatOrNull() ?: 0f
            uploadMap(UPLOAD_MAP, bitmap, mapInfo) { msg, mapInfo ->
                if (mapInfo != null) {
                    dismiss()
                    mapInfoUpdater.update(mapInfo)
                }
                ContextApi.toast(msg)
            }
        }
        view.findViewById<Button>(R.id.cancel)?.setOnClickListener {
            dismiss()
        }
    }

    protected open fun setOriginPoint() {
        set_origin_point.setOnClickListener {
            if (getPlanSize() || getActualSize()) {
                return@setOnClickListener
            }
            var final = false
            val mapInfo = mapInfo
            mapInfoUpdater.update(mapInfo)
            val addPointDialogFragment = AddPointDialogFragment(mapInfo, object : AddPointDialogFragment.UpdateMapPoint {
                override fun update(x: Float, y: Float, z: Float, flag: Int, name: String?, id: String?) {
                    originPointUpdater.update(x, y, z, flag)
                    if (final || flag == 2) {
                        return
                    }
                    this@UploadMapDialogFragment.mapInfo.originInPlan = if (flag == 1 || x < 0f || y < 0f) {
                        this@UploadMapDialogFragment.mapInfo.originInActual = INVALID_FLOAT_ARRAY
                        INVALID_FLOAT_ARRAY
                    } else if (x < 1f) {
                        this@UploadMapDialogFragment.mapInfo.originInActual = floatArrayOf(
                            x * this@UploadMapDialogFragment.mapInfo.actualSize[0],
                            y * this@UploadMapDialogFragment.mapInfo.actualSize[1],
                            z * this@UploadMapDialogFragment.mapInfo.actualSize[2]
                        )
                        floatArrayOf(
                            x * this@UploadMapDialogFragment.mapInfo.planSize[0],
                            y * this@UploadMapDialogFragment.mapInfo.planSize[1],
                            z * this@UploadMapDialogFragment.mapInfo.planSize[2]
                        )
                    } else {
                        this@UploadMapDialogFragment.mapInfo.originInActual = floatArrayOf(
                            x / this@UploadMapDialogFragment.mapInfo.planSize[0] * this@UploadMapDialogFragment.mapInfo.actualSize[0],
                            y / this@UploadMapDialogFragment.mapInfo.planSize[1] * this@UploadMapDialogFragment.mapInfo.actualSize[1],
                            z / this@UploadMapDialogFragment.mapInfo.planSize[2] * this@UploadMapDialogFragment.mapInfo.actualSize[2]
                        )
                        floatArrayOf(x, y, z)
                    }
                }

                override fun update(x: Float, y: Float, z: Float, xF: Float, yF: Float, zF: Float, flag: Int, name: String?, id: String?) {
                    originPointUpdater.update(x, y, z, xF, yF, zF, flag)
                    if (final || flag == 2) {
                        return
                    }
                    this@UploadMapDialogFragment.mapInfo.originInPlan = if (flag == 1 || x < 0f || y < 0f) {
                        this@UploadMapDialogFragment.mapInfo.originInActual = INVALID_FLOAT_ARRAY
                        INVALID_FLOAT_ARRAY
                    } else {
                        this@UploadMapDialogFragment.mapInfo.originInActual = floatArrayOf(xF, yF, zF)
                        floatArrayOf(x, y, z)
                    }
                }
            })
            addPointDialogFragment.dismissListener = DialogInterface.OnDismissListener {
                this.dialog?.show()
                final = true
                addPointDialogFragment.updatePoint(1)
            }
            addPointDialogFragment.show(childFragmentManager, "addPoint")
            dialog?.hide()
        }
    }

    protected open fun getPlanSize(): Boolean {
        val x1 = map_bitmap_size_x.text?.toString()?.toFloatOrNull() ?: return returnToast2("bitmap's width should be integer", true)
        val y1 = map_bitmap_size_y.text?.toString()?.toFloatOrNull() ?: return returnToast2("bitmap's height should be integer", true)
        val z1 = map_bitmap_size_z.text?.toString()?.toFloatOrNull() ?: 0f
        mapInfo.planSize = floatArrayOf(x1, y1, z1)
        return false
    }

    protected open fun getActualSize(): Boolean {
        val x2 = map_actual_size_x.text?.toString()?.toFloatOrNull() ?: return returnToast2("actual's width should be floating number or percentage", true)
        val y2 = map_actual_size_y.text?.toString()?.toFloatOrNull() ?: return returnToast2("actual's height should be floating number or percentage", true)
        val z2 = map_actual_size_z.text?.toString()?.toFloatOrNull() ?: -1f
        mapInfo.actualSize = floatArrayOf(x2, y2, z2)
        return false
    }

    protected open fun getAngleRotation(): Boolean {
        val x2 = map_rotation_x.text?.toString()?.toFloatOrNull() ?: return returnToast2("x-axis rotation should be floating number or percentage", true)
        val y2 = map_rotation_y.text?.toString()?.toFloatOrNull() ?: return returnToast2("y-axis rotation should be floating number or percentage", true)
        val z2 = map_rotation_z.text?.toString()?.toFloatOrNull() ?: return returnToast2("z-axis rotation should be floating number or percentage", true)
        mapInfo.rotationAngle = floatArrayOf(x2, y2, z2)
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            IMAGE_RESULT_CODE -> if (resultCode == Activity.RESULT_OK) {
                val uri: Uri = data?.data ?: return returnToast3("data.data is null Uri")
                bitmap = getBitmapFromUri(uri, this.context ?: return returnToast3("getBitmapFromUri failed because context is null"))
                    ?: return returnToast3("getBitmapFromUri failed")
                map_bitmap_size_x.setText(bitmap!!.width.toString())
                map_bitmap_size_y.setText(bitmap!!.height.toString())
                mapInfo.planSize = floatArrayOf(bitmap!!.width.toFloat(), bitmap!!.height.toFloat(), 0f)
                bitmapUpdater.update(bitmap!!)
            }
        }
    }

    interface UpdateMapBitmap {
        fun update(bitmap: Bitmap, load: Boolean = false)
    }

    interface UpdateMapInfo {
        fun update(mapInfo: DeepNaviMap)
    }

    companion object {
        const val IMAGE_RESULT_CODE = 1

        fun getBitmapFromUri(uri: Uri?, mContext: Context): Bitmap? = try {
            MediaStore.Images.Media.getBitmap(mContext.contentResolver, uri)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
