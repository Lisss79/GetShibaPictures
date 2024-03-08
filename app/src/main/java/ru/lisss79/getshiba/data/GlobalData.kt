package ru.lisss79.getshiba.data

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState

object GlobalData {
    var picRealX = 0f
    var picRealY = 0f
    var picRealWidth = 0f
    var picRealHeight = 0f
    var parentWidth = 0f
    var parentHeight = 0f
    @OptIn(ExperimentalFoundationApi::class)
    var picGridState = LazyStaggeredGridState()
}