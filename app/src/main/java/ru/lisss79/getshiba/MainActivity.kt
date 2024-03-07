package ru.lisss79.getshiba

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animation
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.lisss79.getshiba.ui.theme.GetShibaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GetShibaTheme {
                ScreenSelector(maxPics = 10, this)
            }
        }
    }
}

@Composable
fun ScreenSelector(maxPics: Int, context: Context) {
    var screen by rememberSaveable { mutableStateOf(1) }
    val goBack = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            screen = 1
        }
    }
    val goHome = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            (context as MainActivity).finish()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        var pic: ImageBitmap? by rememberSaveable { mutableStateOf(null) }
        val onPicClick = { selectedPic: ImageBitmap ->
            pic = selectedPic
            screen = 2
        }
        if (screen == 1) {
            BaseScreen(maxPics = maxPics, onPicClick = onPicClick)
            (context as MainActivity).onBackPressedDispatcher.addCallback(goHome)
        } else {
            ShowPicture(pic, context)
            (context as MainActivity).onBackPressedDispatcher.addCallback(goBack)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShowPicture(pic: ImageBitmap?, context: Context) {
    val defPic = ImageBitmap.imageResource(id = R.drawable.ic_clear)
    val picture = pic ?: defPic
    val dm = context.resources.displayMetrics
    val screenWidth = dm.widthPixels
    val screenHeight = dm.heightPixels
    var isTouched = false
    val initialScale =
        if (screenWidth / screenHeight <= picture.width.toFloat() / picture.height.toFloat())
            screenWidth / picture.width.toFloat()
        else screenHeight / picture.height.toFloat()

    var picScale by rememberSaveable { mutableStateOf(1f) }
    var currentOffsetX by rememberSaveable { mutableStateOf(0f) }
    var currentOffsetY by rememberSaveable { mutableStateOf(0f) }
    val animationX by animateFloatAsState(
        targetValue = currentOffsetX,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "animX"
    )
    val animationY by animateFloatAsState(
        targetValue = currentOffsetY,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "animY"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    )
    {
        Image(
            bitmap = picture,
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
            modifier = Modifier
                .combinedClickable(onDoubleClick = {
                    picScale = 1f
                    currentOffsetX = 0f
                    currentOffsetY = 0f
                }, onClick = {
                    isTouched = true
                })
                .graphicsLayer(
                    scaleX = picScale,
                    scaleY = picScale,
                    //translationX = currentOffsetX,
                    translationX = animationX,
                    //translationY = currentOffsetY
                    translationY = animationY
                )
                .onGloballyPositioned {
                    GlobalData.apply {
                        picRealWidth = it.size.width.toFloat() * picScale
                        picRealHeight = it.size.height.toFloat() * picScale
                        picRealX = it.positionInWindow().x
                        picRealY = it.positionInWindow().y
                        println("x= $picRealX, y= $picRealY, w= $picRealWidth, h= $picRealHeight")

                        // Если картинка по обоим размерам меньше экрана, увеличить ее масштаб
                        if (picRealWidth < screenWidth && picRealHeight < screenHeight) {
                            picScale *= 1.1f
                        }
                        // Сместить картинку к краям, если ее утянули дальше
                        else {
                            if (picRealWidth > screenWidth
                                && picRealX > 0
                            ) currentOffsetX -= picScale
                            if (picRealWidth > screenWidth
                                && picRealX + picRealWidth < screenWidth
                            ) currentOffsetX += picScale
                            if (picRealHeight > screenHeight
                                && picRealY > 0
                            ) currentOffsetY -= picScale
                            if (picRealHeight > screenHeight
                                && picRealY + picRealHeight < screenHeight
                            ) currentOffsetY += picScale
                        }

                        // Если картинка по одному размеру меньше экрана, она должна переместиться в центр
                        if (picRealWidth < screenWidth && currentOffsetX > 0)
                            currentOffsetX = 0f
                        if (picRealWidth < screenWidth && currentOffsetX < 0)
                            currentOffsetX = 0f
                        if (picRealHeight < screenHeight && currentOffsetY > 0)
                            currentOffsetY = 0f
                        if (picRealHeight < screenHeight && currentOffsetY < 0)
                            currentOffsetY = 0f

                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, rotation ->
                        picScale *= zoom
                        val offsetFromPanX = pan.x * picScale
                        val offsetFromPanY = pan.y * picScale
                        val newOffsetX = currentOffsetX + offsetFromPanX
                        val newOffsetY = currentOffsetY + offsetFromPanY

                        // Перемещение по горизонтали картинки размером шире экрана
                        if (GlobalData.picRealWidth > screenWidth) {
                            if (GlobalData.picRealX < 0 && pan.x > 0) {
                                currentOffsetX = newOffsetX
                                println("ScreenW= $screenWidth, PicW= ${GlobalData.picRealWidth}")
                                println("New X1")
                            }
                            if (((GlobalData.picRealX + GlobalData.picRealWidth) > screenWidth)
                                && pan.x < 0
                            ) {
                                currentOffsetX = newOffsetX
                                println("ScreenW= $screenWidth, PicW= ${GlobalData.picRealWidth}")
                                println("New X2")
                            }
                        }

                        // Перемещение по вертикали картинки размером выше экрана
                        if (GlobalData.picRealHeight > screenHeight) {
                            if (GlobalData.picRealY < 0 && pan.y > 0) {
                                currentOffsetY = newOffsetY
                                println("ScreenH= $screenHeight, PicW= ${GlobalData.picRealHeight}")
                                println("New Y1")
                            }
                            if (((GlobalData.picRealY + GlobalData.picRealHeight) > screenHeight)
                                && pan.y < 0
                            ) {
                                currentOffsetY = newOffsetY
                                println("ScreenH= $screenHeight, PicW= ${GlobalData.picRealHeight}")
                                println("New Y2")
                            }
                        }
                    }
                },
            contentDescription = "Shiba Big Picture"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseScreen(maxPics: Int, onPicClick: (ImageBitmap) -> Unit) {
    val quantity = rememberSaveable { mutableStateOf(maxPics) }
    var picState by rememberSaveable { mutableStateOf(listOf<Bitmap>()) }
    var picsLoaded by rememberSaveable { mutableStateOf(false) }
    var loading by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val scope1 = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        TextField(
            value = quantity.value.toString(),
            onValueChange = {
                try {
                    quantity.value = it.toInt()
                } catch (e: NumberFormatException) {
                    e.printStackTrace()
                }
            },
            Modifier.fillMaxWidth(),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Number
            ),
            label = {
                Text(text = "How match Shiba?")
            }
        )
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            if (picsLoaded) ImageList(bitmaps = picState, onPicClick)
            if (loading) ProgressIndicator()
            Button(
                onClick = {
                    loading = true
                    //val scope = CoroutineScope(Dispatchers.IO)
                    scope1.launch(Dispatchers.IO) {
                        val urls = NetUtils.getLinks(quantity.value)
                        val bitmaps = if (urls != null) {
                            List(urls.size) { NetUtils.getBitmap(urls[it]) }
                        } else listOf()
                        picState = bitmaps
                        picsLoaded = true
                        loading = false
                    }
                },
                Modifier.align(Alignment.BottomCenter)
            ) {
                Text(
                    text = "Get Shiba!"
                )
            }
        }
    }

}

@Composable
fun ProgressIndicator() {
    Box(modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.Center)
                .width(64.dp)
                .padding(bottom = 48.dp),
            color = MaterialTheme.colorScheme.tertiary,
            strokeWidth = 8.dp
        )
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageList(bitmaps: List<Bitmap>, onPicClick: (ImageBitmap) -> Unit) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(count = 2),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(bitmaps.size) { index ->
            Image(
                bitmap = bitmaps[index].asImageBitmap(),
                contentDescription = "Shiba picture",
                Modifier
                    .padding(2.dp)
                    .clickable {
                        onPicClick.invoke(bitmaps[index].asImageBitmap())
                    },
                contentScale = ContentScale.Inside
            )
        }
    }
}
