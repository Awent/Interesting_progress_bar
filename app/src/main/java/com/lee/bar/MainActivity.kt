package com.lee.bar

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.lee.bar.ProgressView.ProgressListener

/**
 * 一个有趣的矩阵进度条
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val contentTv : TextView = findViewById(R.id.content)
        val pro: ProgressView = findViewById(R.id.progressView)
        pro.apply {
//            setStartAngle(-90)//90°角开始
//            setProBackground(R.mipmap.progress_load_bg)
//            setColor(R.color.colorAccent)
            setProgressListener(object : ProgressListener {
                override fun over() {
                    Log.i("MainActivity", "倒计时结束")
                }

                override fun start() {
                    Log.i("MainActivity", "倒计时开始")
                    contentTv.text = "当前进度：${getCurrentSecond()}"
                }

                override fun progress(total: Int, progress: Float) {
                    val currentPro = total - progress.toInt()
                    contentTv.text = "当前进度：$currentPro"
                    Log.i("MainActivity", "当前剩余进度 = $currentPro")
                }

            })
//            start(60)//60秒
        }

        findViewById<Button>(R.id.start).setOnClickListener{
            pro.reSet()
            pro.start()
        }

        findViewById<Button>(R.id.pause).setOnClickListener{
            pro.pause()
        }

        findViewById<Button>(R.id.reset).setOnClickListener{
            pro.reSet()
        }
    }
}