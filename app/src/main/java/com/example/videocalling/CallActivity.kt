package com.example.videocalling

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.example.videocalling.Models.MessageModel
import com.example.videocalling.databinding.ActivityCallBinding
import com.example.videocalling.util.NewMessageInterface
import com.google.android.material.tabs.TabLayout.TabGravity
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

private const val TAG = "CallActivity"
class CallActivity : AppCompatActivity(), NewMessageInterface {

    lateinit var binding: ActivityCallBinding
    private var userName:String?=null
    private var socketRepositry:SocketRepositry?=null
    private var rtcClient: RTCClient?=null
    private var target:String = ""
    private val gson = Gson()

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
                rtcClient?.addIceCandidate(p0);
                val candidate = hashMapOf(
                    "sdpMid" to p0?.sdpMid,
                    "sdpMLineIndex" to p0?.sdpMLineIndex,
                    "sdpCandidate" to p0?.sdp
                )

                socketRepositry?.sendMessageToSocket(
                    MessageModel("ice_candidate",userName,target,candidate)
                )
            }

            override fun onAddStream(p0: MediaStream?) {
                super.onAddStream(p0)
                p0?.videoTracks?.get(0)?.addSink(binding.remoteView)
            }

        })

        binding.apply {
            callBtn.setOnClickListener {
                socketRepositry?.sendMessageToSocket(
                    MessageModel(
                    "start_call",userName,targetUserNameEt.text.toString(),null
                ))
                target = targetUserNameEt.text.toString()
            }
        }
    }


    override fun onNewMessage(message: MessageModel) {
        Log.d(TAG,"onNewMessage: $message")
        when(message.type){
            "call_response"->{
                if (message.data == "user is not online"){
                    //User is not reachable
                    runOnUiThread {
                        Toast.makeText(this,"User is not reachable",Toast.LENGTH_LONG).show()
                    }
                }else{
                    //we are ready for call,we started a call
                    runOnUiThread{
                        setWhoToCallLayoutGone()
                        setCallLayoutVisible()
                        binding.apply {
                            rtcClient?.initializeSurfaceView(localView)
                            rtcClient?.initializeSurfaceView(remoteView)
                            rtcClient?.startLocalVideo(localView)
                            rtcClient?.call(targetUserNameEt.text.toString())
                        }
                    }
                }
            }
            "answer_received" -> {
                val session = SessionDescription(
                    SessionDescription.Type.ANSWER,
                    message.data.toString()
                )
                rtcClient?.onRemoteSessionReceived(session)
                binding.remoteViewLoading.visibility = View.GONE
            }
            "offer_received" ->{
                runOnUiThread {
                    setIncomingCallLayoutVisible()
                    binding.incomingNameTV.text = "${message.name.toString()} is calling you"
                    binding.acceptButton.setOnClickListener {
                        setIncomingCallLayoutGone()
                        setCallLayoutVisible()
                        setWhoToCallLayoutGone()

                         binding.apply {
                             rtcClient?.initializeSurfaceView(localView)
                             rtcClient?.initializeSurfaceView(remoteView)
                             rtcClient?.startLocalVideo(localView)
                         }
                        val session = SessionDescription(
                            SessionDescription.Type.OFFER,
                            message.data.toString()
                        )
                        rtcClient?.onRemoteSessionReceived(session)
                        rtcClient?.answer(message.name.toString())
                    }
                    binding.rejectButton.setOnClickListener {
                        setIncomingCallLayoutGone()
                    }
                }

            }
            "ice_candidate" ->{
                runOnUiThread {
                    try{
                        val recevingCandidate = gson.fromJson(gson.toJson(message.data),IceCandidateModel::class.java)
                        rtcClient?.addIceCandidate(IceCandidate(recevingCandidate.sdpMid,
                        Math.toIntExact(recevingCandidate.sdpMLineIndex.toLong()),recevingCandidate.sdpCandidate))
                    }catch (e:Exception){
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    private fun setIncomingCallLayoutGone(){
        binding.incomingCallLayout.visibility = View.GONE
    }
    private fun setIncomingCallLayoutVisible(){
        binding.incomingCallLayout.visibility = View.VISIBLE
    }
    private fun setCallLayoutGone(){
        binding.callLayout.visibility = View.GONE
    }
    private fun setCallLayoutVisible(){
        binding.callLayout.visibility = View.VISIBLE
    }
    private fun setWhoToCallLayoutGone(){
        binding.whoToCallLayout.visibility = View.GONE
    }
    private fun setWhoToCallLayoutVisible(){
        binding.whoToCallLayout.visibility = View.VISIBLE
    }
}