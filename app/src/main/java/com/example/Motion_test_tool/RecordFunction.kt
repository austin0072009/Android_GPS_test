package com.example.Motion_test_tool

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_record_function.*
import java.io.*


class RecordFunction : AppCompatActivity() {

    var content: String = ""
    var curTime :String ?= null
    val file = File("data/data/com.example.sensortest/GPS_DATA_RECORD.txt")

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_function)

        var it = getIntent()
        curTime = it.getStringExtra("Time")
        Record_Time.text = "记录了"+curTime+  "秒的GPS信息"

        btn_listener()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
    }

    fun btn_listener()
    {
        Data_Btn1.setOnClickListener {
            val intent = Intent();
            intent.setClass(this, MotionFunction::class.java)
            startActivity(intent)
        }
        Data_Btn2.setOnClickListener{

            read()

        }
        Data_Btn3.setOnClickListener{

           // shareWechatFriend(this, file)
            read()
            val it = Intent(Intent.ACTION_SEND)

            it.putExtra(Intent.EXTRA_TEXT, content)
            it.setType("text/plain")
            startActivity(Intent.createChooser(it, "Choose the transfer method"))

        }
        Data_Btn4.setOnClickListener{

            showTextView.setText("")

        }
    }

    private fun read() {
        try {
            val inputStream = FileInputStream(file)
            val bytes = ByteArray(1024)
            val arrayOutputStream = ByteArrayOutputStream()
            while (inputStream.read(bytes) !== -1) {
                arrayOutputStream.write(bytes, 0, bytes.size)
            }
            inputStream.close()
            arrayOutputStream.close()
            content = String(arrayOutputStream.toByteArray())
            showTextView.setText(content)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}