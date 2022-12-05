package com.example.videocalling

import android.util.Log
import com.example.videocalling.Models.MessageModel
import com.example.videocalling.util.NewMessageInterface
import com.google.gson.Gson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake

import java.net.URI
import kotlin.Exception

private const val TAG = "SocketRepositry"
class SocketRepositry (private val messageInteface:NewMessageInterface){

    private var webSocket:WebSocketClient?=null
    private var userName:String?=null
    private val gson = Gson()
    fun initSocket(username:String){
        userName = username
        //if you are using android emulator your local websocket address is going to be "ws://10.0.2.2:3000"
        //if you are using your phone as emulator your local address is going to be this : "ws://192.168.1.3:3000"
        // but if your websocket is deployed you add your websocket address here
        Log.d(TAG, "initSocket: $username")
        webSocket = object :WebSocketClient(URI("ws://10.0.2.2:3000")){
            override fun onOpen(handshakedata: ServerHandshake?) {
                sendMessageToSocket(MessageModel(
                    "store_user",username,null,null
                ))
            }

            override fun onMessage(message: String?) {
                try{
                    messageInteface.onNewMessage(gson.fromJson(message,MessageModel::class.java))
                } catch (e:Exception){
                    e.printStackTrace()
                }

            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.d(TAG,"OnClose: $reason")
            }

            override fun onError(ex: Exception?) {
                Log.d(TAG,"onError: $ex")
            }

        }
        webSocket?.connect()
    }

    fun sendMessageToSocket(message: MessageModel){
        try{
            Log.d(TAG, "sendMessageToSocket: $message")
            webSocket?.send(Gson().toJson(message))
        } catch (e:Exception){
            e.printStackTrace()
        }
    }

}