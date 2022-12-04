package com.example.videocalling

import org.webrtc.IceCandidate

data class IceCandidateModel (
    val sdpMid:String,
    val sdpMLineIndex:Double,
    val sdpCandidate:String
        )
