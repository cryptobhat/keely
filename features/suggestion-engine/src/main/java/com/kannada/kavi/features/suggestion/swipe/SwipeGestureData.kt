package com.kannada.kavi.features.suggestion.swipe

import android.graphics.PointF

/**
 * SwipeGesture data class for the suggestion engine
 * Mirrors the SwipeGesture from keyboard-view module
 */
data class SwipeGesture(
    val path: List<PointF>,
    val normalizedPath: List<SwipeAlgorithms.NormalizedPoint> = emptyList(),
    val resampledPath: List<SwipeAlgorithms.NormalizedPoint> = emptyList()
)