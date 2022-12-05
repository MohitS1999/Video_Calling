package com.example.videocalling

import android.app.Application
import android.util.Log
import android.view.Surface
import com.example.videocalling.Models.MessageModel
import org.webrtc.*

private const val TAG = "RTCClient"
class RTCClient (
    private val application: Application,
    private val username:String,
    private val socketRepositry:SocketRepositry,
    private val observer: PeerConnection.Observer
    ){

    init {
        initPeerConnectionFactory(application)
    }

    private val eglContext = EglBase.create()
    private val peerConnectionFactory  by lazy { createPeerConnectionFactory() }
    private val iceServer = listOf(
        PeerConnection.IceServer.builder("stun:iphone-stun.strato-iphone.de:3478").createIceServer(),
        PeerConnection.IceServer("stun:openrelay.metered.ca:80"),
        PeerConnection.IceServer("turn:openrelay.metered.ca:80","openrelayproject","openrelayproject"),
        PeerConnection.IceServer("turn:openrelay.metered.ca:443","openrelayproject","openrelayproject"),
        PeerConnection.IceServer("turn:openrelay.metered.ca:443?transport=tcp","openrelayproject","openrelayproject"),

        )
    private val peerConnection by lazy { createPeerConnection(observer) }
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }
    private var videoCapturer : CameraVideoCapturer?=null
    private var localAudioTrack:AudioTrack?=null
    private var localVideoTrack:VideoTrack?=null

    private fun initPeerConnectionFactory(application: Application){
        Log.d(TAG, "initPeerConnectionFactory: ")
        val peerConnectionOption = PeerConnectionFactory.InitializationOptions.builder(application)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(peerConnectionOption)
    }

    private fun createPeerConnectionFactory():PeerConnectionFactory{
        Log.d(TAG, "createPeerConnectionFactory: ")
        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglContext.eglBaseContext,true,true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglContext.eglBaseContext))
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = true
                disableNetworkMonitor = true
            }).createPeerConnectionFactory()
    }
    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection?{
        Log.d(TAG,"createPeerConnection: Creating the peer connectiong ")
        return peerConnectionFactory.createPeerConnection(iceServer,observer)
    }

    fun initializeSurfaceView(surface: SurfaceViewRenderer){
        Log.d(TAG, "initializeSurfaceView: ")
        surface.run{
            setEnableHardwareScaler(true)
            setMirror(true)
            init(eglContext.eglBaseContext,null)
        }
    }

    fun startLocalVideo(surface: SurfaceViewRenderer){
        Log.d(TAG, "startLocalVideo: ")
        val surfaceTextureHelper =
            SurfaceTextureHelper.create(Thread.currentThread().name,eglContext.eglBaseContext)
        videoCapturer = getVideoCapturer(application)
        videoCapturer?.initialize(surfaceTextureHelper,
        surface.context,localVideoSource.capturerObserver)
        videoCapturer?.startCapture(320,240,30)
        localVideoTrack = peerConnectionFactory.createVideoTrack("local_track",localVideoSource)
        localVideoTrack?.addSink(surface)
        localAudioTrack = peerConnectionFactory.createAudioTrack("local_track_audio",localAudioSource)
        val localStream = peerConnectionFactory.createLocalMediaStream("local_stream")
        localStream.addTrack(localAudioTrack)
        localStream.addTrack(localVideoTrack)
        peerConnection?.addStream(localStream)
    }

    private fun getVideoCapturer(application: Application):CameraVideoCapturer{
        Log.d(TAG, "getVideoCapturer: ")
        return Camera2Enumerator(application).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it,null)
            }?:throw
                    java.lang.IllegalStateException()
        }
    }
    fun call(target: String){
        val mediaConstraints = MediaConstraints()
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo","true"))

        peerConnection?.createOffer(object  : SdpObserver{
            override fun onCreateSuccess(desc: SessionDescription?) {
                Log.d(TAG, " call:- onCreateSuccess: createOffer")
                peerConnection?.setLocalDescription(object :SdpObserver{
                    override fun onCreateSuccess(p0: SessionDescription?) {

                    }

                    override fun onSetSuccess() {
                        Log.d(TAG, "call:- onSetSuccess: setLocalDesciption")
                        val offer = hashMapOf(
                            "sdp" to desc?.description,
                            "type" to desc?.type
                        )
                        socketRepositry.sendMessageToSocket(MessageModel(
                            "create_offer",username,target,offer
                        ))
                    }

                    override fun onCreateFailure(p0: String?) {

                    }

                    override fun onSetFailure(p0: String?) {

                    }

                },desc)
            }

            override fun onSetSuccess() {

            }

            override fun onCreateFailure(p0: String?) {

            }

            override fun onSetFailure(p0: String?) {

            }

        },mediaConstraints)
    }

    fun onRemoteSessionReceived(session:SessionDescription){
        peerConnection?.setRemoteDescription(object :SdpObserver{
            override fun onCreateSuccess(p0: SessionDescription?) {

            }

            override fun onSetSuccess() {

            }

            override fun onCreateFailure(p0: String?) {

            }

            override fun onSetFailure(p0: String?) {

            }

        },session)
    }
    fun answer(target:String){
        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo","true"))
        peerConnection?.createAnswer(object :SdpObserver{
            override fun onCreateSuccess(desc: SessionDescription?) {
                Log.d(TAG, "answer:-  onCreateSuccess: ")
                 peerConnection?.setLocalDescription(object : SdpObserver{
                     override fun onCreateSuccess(p0: SessionDescription?) {
                         TODO("Not yet implemented")
                     }

                     override fun onSetSuccess() {
                         Log.d(TAG, "answer:- onSetSuccess: ")
                         val answer = hashMapOf(
                             "sdp" to desc?.description,
                             "type" to desc?.type
                         )
                         socketRepositry.sendMessageToSocket(
                             MessageModel(
                             "create_answer",username,target,answer
                         )
                         )
                     }

                     override fun onCreateFailure(p0: String?) {

                     }

                     override fun onSetFailure(p0: String?) {

                     }

                 },desc)
            }

            override fun onSetSuccess() {

            }

            override fun onCreateFailure(p0: String?) {

            }

            override fun onSetFailure(p0: String?) {

            }

        },constraints)
    }

    fun addIceCandidate(p0: IceCandidate?) {
        peerConnection?.addIceCandidate(p0)
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }

    fun toggleAudio(mute: Boolean) {
        localAudioTrack?.setEnabled(mute)
    }

    fun toggleCamera(cameraPause: Boolean) {
        localVideoTrack?.setEnabled(cameraPause)
    }

    fun endCall() {
        peerConnection?.close()
    }
}