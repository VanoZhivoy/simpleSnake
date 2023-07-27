package ru.oskit.mobile.composetests

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.outlined.ArrowCircleDown
import androidx.compose.material.icons.outlined.ArrowCircleLeft
import androidx.compose.material.icons.outlined.ArrowCircleRight
import androidx.compose.material.icons.outlined.ArrowCircleUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.oskit.mobile.composetests.ui.theme.ComposeTestsTheme
import ru.oskit.mobile.composetests.ui.theme.grayEl
import ru.oskit.mobile.composetests.ui.theme.greenBg
import kotlin.random.Random

data class SnakeState(
    val food: Pair<Int, Int>,
    val snake: List<Triple<Int, Int, BodyDir>>,
    val length: Int,
)
enum class BodyDir{
    UP,
    DOWN,
    LEFT,
    RIGHT
}

const val boardSize = 16

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val logic = Logic(lifecycleScope)

        setContent {
            ComposeTestsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = greenBg
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {

                        val snakeState by logic.snake.collectAsState()

                        Text(
                            text = "Score: " + snakeState.length.toString(),
                            modifier = Modifier.padding(start = 30.dp, top = 10.dp),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = grayEl
                        )

                        AnimatedBox(
                            modifier = Modifier
                                .padding(20.dp)
                                .size(300.dp, 300.dp)
                                .border(width = 2.dp, color = grayEl)
                                .align(CenterHorizontally),
                            state = snakeState
                        )

                        Buttons(
                            modifier = Modifier.align(CenterHorizontally),
                            left = { logic.turnLeft() },
                            right = { logic.turnRight() },
                            up = { logic.turnUp() },
                            down = { logic.turnDown() },
                            start = { logic.startNewGame() }
                        )
                }
            }
        }
    }
}

@Composable
fun AnimatedBox(
    modifier: Modifier = Modifier,
    state: SnakeState,
){
    Canvas(
        modifier = modifier
    ){

            val tileSize = size.width / boardSize
            val sp = state.snake
        sp.forEachIndexed {ind, it ->

            val newPos = Offset(it.first * tileSize,it.second * tileSize )
            if(ind == 0) {
                when (it.third) {
                    BodyDir.UP -> snakeUp(newPos, tileSize)
                    BodyDir.DOWN -> snakeDown(newPos, tileSize)
                    BodyDir.RIGHT -> snakeRight(newPos, tileSize)
                    BodyDir.LEFT -> snakeLeft(newPos, tileSize)
                }
            }else if(it.third != sp[ind - 1].third){
                when{
                    it.third == BodyDir.UP && sp[ind - 1].third == BodyDir.RIGHT -> snakeBodyRect(newPos, tileSize, 180f)
                    it.third == BodyDir.UP && sp[ind - 1].third == BodyDir.LEFT -> snakeBodyRect(newPos, tileSize, 270f)
                    it.third == BodyDir.RIGHT && sp[ind - 1].third == BodyDir.UP -> snakeBodyRect(newPos, tileSize, 0f)
                    it.third == BodyDir.RIGHT && sp[ind - 1].third == BodyDir.DOWN -> snakeBodyRect(newPos, tileSize, 270f)
                    it.third == BodyDir.DOWN && sp[ind - 1].third == BodyDir.LEFT -> snakeBodyRect(newPos, tileSize, 0f)
                    it.third == BodyDir.DOWN && sp[ind - 1].third == BodyDir.RIGHT -> snakeBodyRect(newPos, tileSize, 90f)
                    it.third == BodyDir.LEFT && sp[ind - 1].third == BodyDir.UP -> snakeBodyRect(newPos, tileSize, 90f)
                    it.third == BodyDir.LEFT && sp[ind - 1].third == BodyDir.DOWN -> snakeBodyRect(newPos, tileSize, 180f)
                }

            }else {
                when (it.third) {
                    BodyDir.UP -> snakeBodyVert(newPos, tileSize)
                    BodyDir.DOWN -> snakeBodyVert(newPos, tileSize)
                    BodyDir.RIGHT -> snakeBodyHor(newPos, tileSize)
                    BodyDir.LEFT -> snakeBodyHor(newPos, tileSize)
                }
            }
            //snakeLeft(newPos, tileSize)
        }
        food(
            p = Offset(state.food.first * tileSize, state.food.second * tileSize),
            ts = tileSize
        )
    }
}

    private fun DrawScope.snakeRight(p: Offset, tileSize: Float){ // p - position, ts - title size
        val path = Path().apply {
            moveTo(p.x, p.y)
            lineTo(p.x + tileSize, p.y + tileSize / 5)
            lineTo(p.x + tileSize, p.y + tileSize - tileSize / 5)
            lineTo(p.x, p.y + tileSize)
            lineTo(p.x, p.y + tileSize - tileSize / 5)
            lineTo(p.x + tileSize / 5, p.y + tileSize - tileSize / 5)
            lineTo(p.x + tileSize / 5, p.y + tileSize / 5)
            lineTo(p.x, p.y + tileSize / 5)
            close()
        }
        drawPath(path = path, color = grayEl,)
    }

    private fun DrawScope.snakeLeft(p: Offset, ts: Float){ // p - position, ts - title size
        val path = Path().apply {
            moveTo(p.x, p.y + ts / 5)
            lineTo(p.x + ts, p.y)
            lineTo(p.x + ts, p.y + ts / 5)
            lineTo(p.x + ts - ts / 5, p.y + ts / 5)
            lineTo(p.x + ts - ts / 5, p.y + ts - ts / 5)
            lineTo(p.x + ts, p.y + ts - ts / 5)
            lineTo(p.x + ts, p.y + ts)
            lineTo(p.x, p.y + ts - ts / 5)
            close()
        }
        drawPath(path = path, color = grayEl)
    }

    private fun DrawScope.snakeUp(p: Offset, ts: Float){ // p - position, ts - title size
        val path = Path().apply {
            moveTo(p.x + ts / 5, p.y)
            lineTo(p.x + ts - ts / 5, p.y)
            lineTo(p.x + ts, p.y + ts)
            lineTo(p.x + ts - ts / 5, p.y + ts)
            lineTo(p.x + ts - ts / 5, p.y + ts - ts / 5)
            lineTo(p.x + ts / 5, p.y + ts - ts / 5)
            lineTo(p.x + ts / 5, p.y + ts)
            lineTo(p.x, p.y + ts)
            close()
        }
        drawPath(path = path, color = grayEl)
    }

    private fun DrawScope.snakeDown(p: Offset, ts: Float){
        val path = Path().apply {
            moveTo(p.x, p.y)
            lineTo(p.x + ts / 5, p.y)
            lineTo(p.x + ts / 5, p.y + ts / 5)
            lineTo(p.x + ts - ts / 5, p.y + ts / 5)
            lineTo(p.x + ts - ts / 5, p.y)
            lineTo(p.x + ts, p.y)
            lineTo(p.x + ts - ts / 5, p.y + ts)
            lineTo(p.x + ts / 5, p.y + ts)
            close()
        }
        drawPath(path = path, color = grayEl)
    }

    private fun DrawScope.snakeBodyHor(
        p: Offset, // position
        ts: Float
    ){
        drawRect(
            color = grayEl,
            topLeft = Offset(p.x, p.y  + ts/5),
            size = Size(ts, ts - ts/5 * 2)
        )
    }

    private fun DrawScope.snakeBodyVert(
        p: Offset, // position
        ts: Float
    ){
        drawRect(
            color = grayEl,
            topLeft = Offset(p.x + ts/5, p.y),
            size = Size(ts - ts / 5 * 2, ts)
        )
    }


    private fun DrawScope.snakeBodyRect(
        p: Offset, // position
        ts: Float,
        rec: Float
    ){

        val path = Path().apply {
            moveTo(p.x, p.y + ts / 5)
            lineTo(p.x + ts / 5, p.y + ts / 5)
            lineTo(p.x + ts / 5, p.y)
            lineTo(p.x + ts - ts / 5, p.y)
            lineTo(p.x + ts - ts / 5, p.y + ts / 5)
            //lineTo(p.x + ts, p.y + ts / 5)
            //lineTo(p.x + ts, p.y + ts - ts / 5)
            //lineTo(p.x + ts - ts / 5, p.y + ts - ts / 5)
            //lineTo(p.x + ts - ts / 5, p.y + ts)
            //lineTo(p.x + ts / 5, p.y + ts)
            lineTo(p.x + ts / 5, p.y + ts - ts / 5)
            lineTo(p.x, p.y + ts - ts / 5)
            close()
        }
        //drawCircle(color = Color.Cyan, center = Offset(p.x + ts / 2, p.y + ts / 2), radius = 7f)
        rotate(rec, Offset(p.x + ts / 2, p.y + ts / 2)){
            drawPath(path = path, color = grayEl)
        }

    }

    private fun DrawScope.food(
        p: Offset,
        ts: Float
    ){
        drawRect(
            color = grayEl,
            topLeft = Offset(p.x, p.y + ts/3),
            size = Size(ts / 3f, ts / 3f)
        )
        drawRect(
            color = grayEl,
            topLeft = Offset(p.x  + ts/3, p.y),
            size = Size(ts / 3f, ts / 3f)
        )
        drawRect(
            color = grayEl,
            topLeft = Offset(p.x  + ts/3, p.y +ts / 3 * 2),
            size = Size(ts / 3f, ts / 3f)
        )
        drawRect(
            color = grayEl,
            topLeft = Offset(p.x + ts/3 * 2, p.y + ts/3),
            size = Size(ts / 3f, ts / 3f)
        )
    }

@Composable
fun Buttons(
    modifier: Modifier = Modifier,
    left: () -> Unit,
    right: () -> Unit,
    up: () -> Unit,
    down: () -> Unit,
    start: () -> Unit,
){
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = CenterHorizontally) {

        IconButton(
            onClick = { up() },
            modifier = Modifier.border(width = 2.dp, color = grayEl, shape = ShapeDefaults.Medium)
        ) {
            Icon(Icons.Outlined.ArrowCircleUp, contentDescription = "up", modifier = Modifier.size(80.dp), tint = grayEl)

        }

        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { left() },
                modifier = Modifier.border(width = 2.dp, color = grayEl, shape = ShapeDefaults.Medium)
            ) {
                Icon(Icons.Outlined.ArrowCircleLeft, contentDescription = "left", modifier = Modifier.size(80.dp), tint = grayEl)
            }

            Spacer(modifier = Modifier.size(100.dp))

            IconButton(
                onClick = { right() },
                modifier = Modifier.border(width = 2.dp, color = grayEl, shape = ShapeDefaults.Medium)
            ) {
                Icon(Icons.Outlined.ArrowCircleRight, contentDescription = "right", modifier = Modifier.size(80.dp), tint = grayEl)
            }
        }

        IconButton(
            onClick = { down() },
            modifier = Modifier.border(width = 2.dp, color = grayEl, shape = ShapeDefaults.Medium)
        ) {
            Icon(Icons.Outlined.ArrowCircleDown, contentDescription = "down", modifier = Modifier.size(80.dp), tint = grayEl)
        }
        Spacer(modifier = Modifier.size(50.dp))
        IconButton(onClick = { start() }, modifier = Modifier.align(Alignment.End)) {
            Icon(Icons.Filled.RestartAlt, contentDescription = "start", modifier = Modifier.size(60.dp), tint = grayEl)
        }
    }
}

    class Logic(private val scope: CoroutineScope){
        private val right = 1 to 0
        private val left = -1 to 0
        private val up = 0 to -1
        private val down = 0 to 1

        private var direct = right

        fun turnLeft(){
            if(direct != right) direct = left
        }

        fun turnRight(){
            if(direct != left) direct = right
        }

        fun turnUp(){
            if(direct != down) direct = up
        }

        fun turnDown(){
            if(direct != up) direct = down
        }

        private val mutex = Mutex()

        private var delay: Long = 0
        private var play = false
        private var snakeLength = 5

        private val _snake = MutableStateFlow(
            SnakeState(
                food = 0 to 0,
                snake = listOf(Triple(7, 7, BodyDir.RIGHT)),
                0
            )
        )
        val snake: StateFlow<SnakeState> = _snake


        fun startNewGame(){
            _snake.value = SnakeState(
                food = Random.nextInt(boardSize) to Random.nextInt(boardSize),
                snake = listOf(Triple(7, 7, BodyDir.RIGHT)),
                0

            )
            direct = right
            snakeLength = 5
            delay = 280
            play = true
            startGame()
        }

        private fun startGame(){
            scope.launch {
                while (play){
                    delay(delay)
                    _snake.update { snakeState ->
                        val snakeHead = snakeState.snake.first()
                        val endBoard = when{
                            snakeHead.first == 0 && direct == -1 to 0 -> true // left board
                            snakeHead.second == 0 && direct == 0 to -1 -> true // up board
                            snakeHead.first == boardSize - 1 && direct == 1 to 0 -> true // right
                            snakeHead.second == boardSize - 1 && direct == 0 to 1 -> true // down
                            else -> false
                        }
                        if(endBoard) {
                            play = false
                            return@launch
                        }

                        val newPosition = snakeHead.let {p ->
                            mutex.withLock {
                                Triple(
                                    (p.first + direct.first) % boardSize,
                                    (p.second + direct.second) % boardSize,
                                    when(direct){
                                        up -> BodyDir.UP
                                        down -> BodyDir.DOWN
                                        left -> BodyDir.LEFT
                                        else -> BodyDir.RIGHT
                                    }
                                )
                            }
                        }

                        val newFood = if(newPosition.first == snakeState.food.first && newPosition.second == snakeState.food.second){
                            snakeLength++
                            delay -= when{
                                delay > 200 -> 20
                                delay > 180 -> 15
                                delay > 150 -> 10
                                delay > 100 -> 5
                                delay <= 10 -> 0
                                else -> 0
                            }
                            true
                        }else{
                            false
                        }

                        val foodPosition = if(newFood){
                            var correctFood = false
                            var foodPosition = 0 to 0
                            while(!correctFood){
                                foodPosition = Random.nextInt(boardSize) to Random.nextInt(boardSize)
                                correctFood = !snakeState.snake.any {
                                    it.first == foodPosition.first && it.second == foodPosition.second
                                }
                            }
                            foodPosition
                        }else{
                            snakeState.food
                        }

                        if (snakeState.snake.any {
                                it.first == newPosition.first && it.second == newPosition.second
                            }
                        ){
                            play = false
                            return@launch
                        }

                        snakeState.copy(
                            food = foodPosition,
                            snake = listOf(newPosition) + snakeState.snake.take(snakeLength - 1),
                            length = snakeLength - 5
                        )
                    }
                }
            }
        }
    }
}

