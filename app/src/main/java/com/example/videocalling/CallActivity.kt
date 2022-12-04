package com.example.videocalling

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.videocalling.Models.MessageModel
import com.example.videocalling.databinding.ActivityCallBinding
import com.example.videocalling.util.NewMessageInterface

class CallActivity : AppCompatActivity(), NewMessageInterface {

    lateinit var binding: ActivityCallBinding
    private var userName:String?=null
    private var socketRepositry:SocketRepositry?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
    }

    private fun init(){
        userName = intent.getStringExtra("username")
        socketRepositry = SocketRepositry(this)
        userName?.let { socketRepositry?.initSocket(it) }
    }


    override fun onNewMessage(message: MessageModel) {

    }
}