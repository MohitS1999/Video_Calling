package com.example.videocalling

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.videocalling.Models.MessageModel
import com.example.videocalling.databinding.ActivityCallBinding
import com.example.videocalling.util.NewMessageInterface
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection

class CallActivity : AppCompatActivity(), NewMessageInterface {

    lateinit var binding: ActivityCallBinding
    private var userName:String?=null
    private var socketRepositry:SocketRepositry?=null
    private var rtcClient: RTCClient?=null

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
        rtcClient = RTCClient(application,userName!!,socketRepositry!!,object :PeerConnectionObserver(){
            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
            }

            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
            }

        })
        rtcClient?.initializeSurfaceView(binding.localView)
        rtcClient?.startLocalVideo(binding.localView)
    }


    override fun onNewMessage(message: MessageModel) {

    }
}