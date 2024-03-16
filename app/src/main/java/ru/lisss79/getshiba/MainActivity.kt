package ru.lisss79.getshiba

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.lisss79.getshiba.data.GlobalData
import ru.lisss79.getshiba.data.PicturesRepository
import ru.lisss79.getshiba.network.NetUtils
import ru.lisss79.getshiba.ui.theme.GetShibaTheme
import kotlin.math.min
import kotlin.system.exitProcess

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

/**
 * Стартовая функция, выбирает какой экран показать - список или картинку
 */
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
            exitProcess(0)
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
            (context as MainActivity).onBackPressedDispatcher.apply {
                addCallback(goHome)
            }
        } else {
            ShowPicture(pic, context)
            (context as MainActivity).onBackPressedDispatcher.apply {
                addCallback(goBack)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun ShowPicture(pic: ImageBitmap?, context: Context) {
    val defPic = ImageBitmap.imageResource(id = R.drawable.ic_clear)
    val picture = pic ?: defPic
    val dm = context.resources.displayMetrics
    val screenWidth = dm.widthPixels
    val screenHeight = dm.heightPixels

    var picScale by rememberSaveable { mutableStateOf(1f) }
    val animationScale = animateFloatAsState(
        targetValue = picScale,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "animScale"
    )
    var currentOffsetX by rememberSaveable { mutableStateOf(0f) }
    var currentOffsetY by rememberSaveable { mutableStateOf(0f) }
    val animationX = animateFloatAsState(
        targetValue = currentOffsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "animX"
    )
    val animationY = animateFloatAsState(
        targetValue = currentOffsetY,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "animY"
    )

    // Box с картинкой внутри
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                GlobalData.parentWidth = it.size.width.toFloat()
                GlobalData.parentHeight = it.size.height.toFloat()
            },
        contentAlignment = Alignment.Center
    )
    {
        // Картинка с шибой
        Image(
            bitmap = picture,
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center,
            modifier = Modifier
                .graphicsLayer(
                    scaleX = animationScale.value,
                    scaleY = animationScale.value,
                    translationX = animationX.value,
                    translationY = animationY.value
                )
                .combinedClickable(onDoubleClick = {
                    picScale = 1f
                    currentOffsetX = 0f
                    currentOffsetY = 0f
                }, onClick = { })
                .onGloballyPositioned {
                    GlobalData.apply {
                        // Позиция и размеры картинки с учетом масштаба
                        picRealWidth = it.size.width.toFloat() * picScale
                        picRealHeight = it.size.height.toFloat() * picScale
                        picRealX = it.positionInParent().x
                        picRealY = it.positionInParent().y

                        // Размеры поля для отображения картинки
                        val boxWidth =
                            if (parentWidth > 0) parentWidth else screenWidth.toFloat()
                        val boxHeight =
                            if (parentHeight > 0) parentHeight else screenHeight.toFloat()

                        // Смещения при нахождении картинки у левого и верхнего краев
                        val leftOffsetX = (picRealWidth - boxWidth) / 2
                        val topOffsetY = (picRealHeight - boxHeight) / 2

                        val animXisActive = (animationX as AnimationState<Float, *>).isRunning
                        val animYisActive = (animationY as AnimationState<Float, *>).isRunning
                        val animScaleIsActive =
                            (animationScale as AnimationState<Float, *>).isRunning
                        // Если картинка по обоим размерам меньше экрана, увеличить ее масштаб
                        println("boxWidth: $boxWidth, picWidth: $picRealWidth")
                        if (picRealWidth < boxWidth && picRealHeight < boxHeight
                            && !animScaleIsActive
                        ) {
                            picScale = min(
                                boxWidth / it.size.width.toFloat(),
                                boxHeight / it.size.height.toFloat()
                            )
                        }
                        // Сместить картинку больше экрана к краям, если ее утянули ближе к центру
                        if (picRealWidth >= boxWidth && !animXisActive) {
                            if (picRealWidth >= boxWidth && picRealX > 0)
                                currentOffsetX = leftOffsetX
                            if (picRealWidth >= boxWidth &&
                                picRealX + picRealWidth < boxWidth
                            ) currentOffsetX = -leftOffsetX
                        }
                        if (picRealHeight >= boxHeight && !animYisActive) {
                            if (picRealHeight >= boxHeight && picRealY > 0)
                                currentOffsetY = topOffsetY
                            if (picRealHeight >= boxHeight &&
                                picRealY + picRealHeight < boxHeight
                            ) currentOffsetY = -topOffsetY
                        }

                        // Если картинка по одному размеру меньше экрана, переместить в центр
                        if (picRealWidth < boxWidth && !animXisActive) currentOffsetX = 0f
                        if (picRealHeight < boxHeight && !animYisActive) currentOffsetY = 0f
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, rotation ->
                        picScale *= zoom
                        val offsetFromPanX = pan.x * picScale
                        val offsetFromPanY = pan.y * picScale

                        // Новое смещение картинки
                        currentOffsetX += offsetFromPanX
                        currentOffsetY += offsetFromPanY
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
    // Column экрана с запросом картинок и выбора из полученных
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Текстовое поле с выбором числа картинок
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
                Text(text = "How many Shibas?")
            }
        )
        // Поле ниже текстового - картинки или индикатор прогресса и кнопка
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                // Индикатор прогресса
                loading -> ProgressIndicator()
                // Список картинок, загруженных сейчас
                picsLoaded && picState.isNotEmpty() -> ImageList(bitmaps = picState, onPicClick)
                // Если картинки уже загружены, показываем их (для кнопки назад)
                PicturesRepository.bitmaps != null ->
                    ImageList(bitmaps = PicturesRepository.bitmaps!!, onPicClick)
                // Пустой экран
                else -> EmptyData()
            }
            // Кнопка "получить картинки"
            Button(
                onClick = {
                    loading = true
                    scope1.launch(Dispatchers.IO) {
                        val urls = NetUtils.getLinks(quantity.value)
                        urls?.run {
                            val bitmaps = mutableListOf<Bitmap>()
                            forEach { url ->
                                NetUtils.getBitmap(url)?.let { pic -> bitmaps.add(pic) }
                            }
                            PicturesRepository.bitmaps = bitmaps
                            picState = bitmaps
                            picsLoaded = true
                            loading = false
                        } ?: run {
                            PicturesRepository.bitmaps = null
                            picState = emptyList()
                            picsLoaded = false
                            loading = false
                        }

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

@Composable
fun EmptyData() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 48.dp),
            color = MaterialTheme.colorScheme.tertiary,
            fontSize = 24.sp,
            text = "No Shiba loaded..."
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageList(bitmaps: List<Bitmap>, onPicClick: (ImageBitmap) -> Unit) {
    val state = GlobalData.picGridState
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(count = 2),
        modifier = Modifier.fillMaxWidth(),
        state = state
    ) {
        items(bitmaps.size) { index ->
            Image(
                bitmap = bitmaps[index].asImageBitmap(),
                contentDescription = "Shiba picture",
                Modifier
                    .padding(2.dp)
                    .clickable {
                        onPicClick.invoke(bitmaps[index].asImageBitmap())
                        GlobalData.picGridState = state
                    },
                contentScale = ContentScale.Inside
            )
        }
    }
}
