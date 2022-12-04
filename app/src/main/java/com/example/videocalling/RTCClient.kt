package com.example.videocalling

import android.app.Application
import android.view.Surface
import org.webrtc.*

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

        )
    private val peerConnection by lazy { createPeerConnection(observer) }
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }
    private fun initPeerConnectionFactory(application: Application){
        val peerConnectionOption = PeerConnectionFactory.InitializationOptions.builder(application)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(peerConnectionOption)
    }

    private fun createPeerConnectionFactory():PeerConnectionFactory{
        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglContext.eglBaseContext,true,true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglContext.eglBaseContext))
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = true
                disableNetworkMonitor = true
            }).createPeerConnectionFactory()
    }
    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection?{
        return peerConnectionFactory.createPeerConnection(iceServer,observer)
    }

    fun initializeSurfaceView(surface: SurfaceViewRenderer){
        surface.run{
            setEnableHardwareScaler(true)
            setMirror(true)
            init(eglContext.eglBaseContext,null)
        }
    }

    fun startLocalVideo(surface: SurfaceViewRenderer){
        val surfaceTextureHelper =
            SurfaceTextureHelper.create(Thread.currentThread().name,eglContext.eglBaseContext)
        val videoCapturer = getVideoCapturer(application)
        videoCapturer.initialize(surfaceTextureHelper,
        surface.context,localVideoSource.capturerObserver)
        videoCapturer.startCapture(320,240,30)
        val localVideoTrack = peerConnectionFactory.createVideoTrack("local_track",localVideoSource)
        localVideoTrack.addSink(surface)
        val localAudioTrack = peerConnectionFactory.createAudioTrack("local_track_audio",localAudioSource)
        val localStream = peerConnectionFactory.createLocalMediaStream("local_stream")
        localStream.addTrack(localAudioTrack)
        localStream.addTrack(localVideoTrack)
        peerConnection?.addStream(localStream)
    }

    private fun getVideoCapturer(application: Application):CameraVideoCapturer{
        return Camera2Enumerator(application).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it,null)
            }?:throw
                    java.lang.IllegalStateException()
        }
    }
}