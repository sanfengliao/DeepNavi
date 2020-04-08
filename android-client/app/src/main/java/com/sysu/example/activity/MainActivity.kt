package com.sysu.example.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sysu.example.R
import kotlinx.android.synthetic.main.activity_main.start_map_modeling
import kotlinx.android.synthetic.main.activity_main.start_navigation
import kotlinx.android.synthetic.main.activity_main.start_train

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        start_navigation.setOnClickListener { startActivity(Intent(this, NaviActivity::class.java)) }
        start_train.setOnClickListener { startActivity(Intent(this, ConfigActivity::class.java)) }
        start_map_modeling.setOnClickListener { startActivity(Intent(this, MapActivity::class.java)) }
    }
}
