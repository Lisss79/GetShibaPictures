package ru.lisss79.getshiba

class CoordsTransform(
    val x: Float,
    val y: Float,
    val picWidth: Float,
    val picHeight: Float,
    val scale: Float,
    val screenWidth: Float,
    val screenHeight: Float
) {
    val screenCenterX: Float
        get() = screenWidth / 2
    val screenCenterY: Float
        get() = screenHeight / 2

    fun getRealX(): Float {
        val realPicWidth = picWidth * scale
        return x + (picWidth - realPicWidth) / 2
    }
    fun getRealY(): Float {
        val realPicHeight = picHeight * scale
        return y + (picHeight - realPicHeight) / 2
    }
}