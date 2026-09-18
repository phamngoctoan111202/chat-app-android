package com.noatnoat.chatapp.webrtc

import android.content.Context
import com.noatnoat.chatapp.core.network.logging.AppLogger
import org.webrtc.*

class WebRtcEngineManager(
    private val context: Context,
    private val onIceCandidateGenerated: (IceCandidate) -> Unit = {},
    private val onConnectionStateChanged: (PeerConnection.PeerConnectionState) -> Unit = {}
) {
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioTrack: AudioTrack? = null
    private var localVideoTrack: VideoTrack? = null
    private var audioSource: AudioSource? = null
    private var videoSource: VideoSource? = null
    private var videoCapturer: VideoCapturer? = null

    private val eglBase: EglBase by lazy { EglBase.create() }

    init {
        initWebRtcFactory()
    }

    private fun initWebRtcFactory() {
        try {
            val initOptions = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(initOptions)

            val options = PeerConnectionFactory.Options()
            peerConnectionFactory = PeerConnectionFactory.builder()
                .setOptions(options)
                .createPeerConnectionFactory()

            AppLogger.d(TAG, "WebRTC PeerConnectionFactory initialized successfully.")
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to initialize WebRTC PeerConnectionFactory", e)
        }
    }

    fun startCall(isVideo: Boolean) {
        val rtcConfig = PeerConnection.RTCConfiguration(
            listOf(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
            )
        ).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        val observer = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate?.let { onIceCandidateGenerated(it) }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                newState?.let { onConnectionStateChanged(it) }
            }

            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {}
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)

        // Setup Audio Track
        peerConnectionFactory?.let { factory ->
            val constraints = MediaConstraints()
            audioSource = factory.createAudioSource(constraints)
            localAudioTrack = factory.createAudioTrack("ARDAMSa0", audioSource)
            localAudioTrack?.setEnabled(true)
            peerConnection?.addTrack(localAudioTrack, listOf("ARDAMS"))
        }

        // Setup Video Track if video call
        if (isVideo) {
            setupVideoCapturer()
        }
    }

    private fun setupVideoCapturer() {
        try {
            val cameraEnumerator = Camera2Enumerator(context)
            val deviceNames = cameraEnumerator.deviceNames
            var frontCameraName: String? = null

            for (deviceName in deviceNames) {
                if (cameraEnumerator.isFrontFacing(deviceName)) {
                    frontCameraName = deviceName
                    break
                }
            }

            val targetCamera = frontCameraName ?: deviceNames.firstOrNull()
            if (targetCamera != null) {
                videoCapturer = cameraEnumerator.createCapturer(targetCamera, null)
                peerConnectionFactory?.let { factory ->
                    videoSource = factory.createVideoSource(false)
                    videoCapturer?.initialize(
                        SurfaceTextureHelper.create("WebRtcVideoThread", eglBase.eglBaseContext),
                        context,
                        videoSource?.capturerObserver
                    )
                    videoCapturer?.startCapture(1280, 720, 30)
                    localVideoTrack = factory.createVideoTrack("ARDAMSv0", videoSource)
                    localVideoTrack?.setEnabled(true)
                    peerConnection?.addTrack(localVideoTrack, listOf("ARDAMS"))
                }
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Failed to start camera capturer", e)
        }
    }

    fun setMute(isMuted: Boolean) {
        localAudioTrack?.setEnabled(!isMuted)
    }

    fun setCameraEnabled(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
        if (enabled) {
            try {
                videoCapturer?.startCapture(1280, 720, 30)
            } catch (e: Exception) {
                AppLogger.w(TAG, "Video capturer resume failed", e)
            }
        } else {
            try {
                videoCapturer?.stopCapture()
            } catch (e: Exception) {
                AppLogger.w(TAG, "Video capturer stop failed", e)
            }
        }
    }

    fun endCall() {
        try {
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            videoCapturer = null

            localAudioTrack?.dispose()
            localVideoTrack?.dispose()

            peerConnection?.close()
            peerConnection = null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error closing WebRTC engine", e)
        }
    }

    companion object {
        private const val TAG = "FLOW_WEBRTC"
    }
}
