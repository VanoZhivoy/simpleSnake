package ru.oskit.mobile.composetests

import android.graphics.Paint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import kotlin.random.Random

data class SnakeState(
    val food: Pair<Int, Int>,
    val snake: List<Pair<Int, Int>>,

)

const val boardSize = 16

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val logic = Logic(lifecycleScope)

        setContent {
            ComposeTestsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box {

                        val snakeState by logic.snake.collectAsState()

                        AnimatedBox(
                            modifier = Modifier
                                .size(300.dp, 300.dp)
                                .background(Color.Blue),
                            state = snakeState
                        )

                        Buttons(
                            modifier = Modifier.align(Alignment.BottomCenter),
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
        modifier = modifier.size(500.dp, 500.dp)
    ){

            val tileSize = size.width / boardSize
            val sp = state.snake
        sp.forEach {
            val newPos = Offset(it.first * tileSize,it.second * tileSize )
            snakeBody(newPos, tileSize)
        }
        food(
            position = Offset(state.food.first * tileSize + tileSize / 2, state.food.second * tileSize + tileSize / 2),
            tileSize = tileSize
        )

    }
}

    private fun DrawScope.snakeBody(
        position: Offset,
        tileSize: Float
    ){
        drawRoundRect(
            brush = Brush.radialGradient(listOf(Color.Red, Color.Yellow, Color.Green)),
            cornerRadius = CornerRadius(5f, 5f),
            size = Size(tileSize, tileSize),
            topLeft = position
        )
    }

    private fun DrawScope.food(
        position: Offset,
        tileSize: Float
    ){
        drawCircle(
            brush = Brush.radialGradient(listOf(Color.Black, Color.Cyan)),
            radius = tileSize / 2f,
            center = position
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
    Row(modifier = modifier){
        Button(onClick = {left()}) {
            Text("left")
        }
        Button(onClick = right) {
            Text("right")
        }
        Button(onClick = up) {
            Text("up")
        }
        Button(onClick = down) {
            Text("down")
        }
        Button(onClick = { start() }) {
            Text("Start")
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

        val mutex = Mutex()
        private val _snake = MutableStateFlow(
            SnakeState(
                food = 0 to 0,
                snake = listOf(7 to 7)
            )
        )
        val snake: StateFlow<SnakeState> = _snake
        private var delay: Long = 0
        private var play = false
        var snakeLength = 3

        fun startNewGame(){
            _snake.value = SnakeState(
                food = Random.nextInt(boardSize) to Random.nextInt(boardSize),
                snake = listOf(7 to 7)
            )
            snakeLength = 4
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
                                Pair(
                                    (p.first + direct.first) % boardSize,
                                    (p.second + direct.second) % boardSize
                                )
                            }
                        }

                        val newFood = if(newPosition == snakeState.food){
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
                                correctFood = !snakeState.snake.contains(foodPosition)
                            }
                            foodPosition
                        }else{
                            snakeState.food
                        }

                        if (snakeState.snake.contains(newPosition)){
                            play = false
                        }

                        snakeState.copy(
                            food = foodPosition,
                            snake = listOf(newPosition) + snakeState.snake.take(snakeLength - 1))
                    }

                    }
                }
            }
        }
    }

