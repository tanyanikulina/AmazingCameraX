package com.example.amazingcameraxapp.data

enum class FlashState {
    FlashStateOn,
    FlashStateOff,
    FlashStateAuto
}

enum class CaptureState {
    VideoState,
    PhotoState
}

enum class AspectRatioState {
    AspectRatio_16_9,
    AspectRatio_4_3
}

enum class CameraSelectorState {
    FrontCameraState,
    BackCameraState
}

enum class ExtensionState {
    ExtensionStateOn,
    ExtensionStateOff
}

enum class AnalyzerState {
    CustomAnalyzerState,
    FaceAnalyzerState,
    PoseAnalyzerState,
    FaceAndPoseAnalyzerState
}
