package com.ascolith.textseek.domain.model

data class TsPoint(
    val x: Float,
    val y: Float,
)

/**
 * A four-cornered outline, with corners named as the text reads, so it can follow text that is
 * tilted in the frame rather than only box it upright.
 */
data class TsQuad(
    val topLeft: TsPoint,
    val topRight: TsPoint,
    val bottomRight: TsPoint,
    val bottomLeft: TsPoint,
) {
    val corners: List<TsPoint> get() = listOf(topLeft, topRight, bottomRight, bottomLeft)
}
