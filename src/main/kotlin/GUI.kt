import javafx.application.Application
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.stage.Stage
import BoardUtils.*
import Base.Game
import Base.Piece
import UI.Config
import UI.VisualController
import player.engines.Capturer
import engines.MasterOfZeroDepth
import javafx.animation.AnimationTimer
import javafx.scene.text.Font
import player.HumanPlayers.VisualPlayer


class GUI : Application() {

    @FXML
    private lateinit var canvas: Canvas
    @FXML
    private lateinit var commandLine: TextField

    private lateinit var stage: Stage
    @FXML
    private lateinit var scene: Scene
    private lateinit var root: Parent
    private lateinit var paintBrush: GraphicsContext

    @FXML
    private  lateinit var log: TextArea
    val vc = VisualController()
    var game = Game(placeHolder =  true)
//    var game = Game() // this will be the .main visual game
//    var visualizer = Visualizer(game) // for extra rendering
//    var parser = Parser(game, visualizer) // for extra manipulation of game and visualizer
    var clickedSquare = -1

    val imageIndexW = mapOf<Int, String>(
        1 to "Piece Images 2/White Pawn.png",
        2 to "Piece Images 2/White Knight.png",
        3 to "Piece Images 2/White Bishop.png",
        4 to "Piece Images 2/White Rook.png",
        5 to "Piece Images 2/White Queen.png",
        6 to "Piece Images 2/White King.png",
    )
    val imageIndexB = mapOf<Int, String>(
        1 to "Piece Images 2/Black Pawn.png",
        2 to "Piece Images 2/Black Knight.png",
        3 to "Piece Images 2/Black Bishop.png",
        4 to "Piece Images 2/Black Rook.png",
        5 to "Piece Images 2/Black Queen.png",
        6 to "Piece Images 2/Black King.png"
        )


    var squarePositions = (Array(8) { Array(8) { Pair(0.0, 0.0) } })
    var mouseX = -1.0
    var mouseY = -1.0


    @FXML
    fun openMenu(e: ActionEvent) {
        Game.Sessions.vacateAll()
        root = FXMLLoader.load(javaClass.getResource("Menu2.fxml"))
        stage = ((e.source as Node).scene.window) as Stage
        scene = Scene(root)
        stage.scene = scene
        stage.show()
    }

    @FXML
    fun newGame() {
        if (game.isOngoing()) game.killThisSession()
        //val game1 = Game(Capturer(), MasterOfZeroDepth())
        //game1.prepareToBegin()
        //game1.begin()

        game = Game(Config.player1, Config.player2)
        vc.switchToGame(game)
        vc.viewFrom(game.player1color)
        game.prepareToBegin()
        game.begin()
        //vc.switchToGame(game1)
        log.text = ""
    }

    @FXML
    fun undoMove() { }

    @FXML
    fun parseCommand() {
        vc.parseText(commandLine.text)
        log.text = vc.getParserOutput().joinToString(" ")
    }

    @Override
    fun initialize() {
        paintBrush = canvas.graphicsContext2D
        setSquarePositions()

        val refreshRate = object : AnimationTimer() {
            private var lastUpdate = 0L
            override fun handle(now: Long) {
                if (now - lastUpdate >= 20_000_000) {
                    lastUpdate = now
                    refreshBoard()
                }
            }
        }
        refreshRate.start()

        canvas.setOnMousePressed { e ->
            mouseX = e.x - 67.5 / 2
            mouseY = e.y - 67.5 / 2
            clickedSquare = convertPairToIntSquare(adjustForOrientation(getSquareFromPixels(e.x,e.y)))
                if (game.board.fetchPiece(clickedSquare).isColor(game.turn)) {
                    vc.addMoveSquareMasks(clickedSquare)
                }
        }

        canvas.setOnMouseDragged { e ->
            mouseX = e.x - 67.5 / 2
            mouseY = e.y - 67.5 / 2
        }

        canvas.setOnMouseReleased { e ->
            val endSquare = convertPairToIntSquare(adjustForOrientation(getSquareFromPixels(e.x,e.y)))
            if (endSquare != -1 && endSquare != clickedSquare) {
                if (playerCanPlay()) {
                        game.receiveHumanMove(clickedSquare, endSquare)
                }
            }
            vc.clearVisuals()
            clickedSquare = -1

        }
    }                                   //    NOTE: listeners are here


    fun playerCanPlay(): Boolean {
        return game.getPlayerOf(game.turn) is VisualPlayer
    }

    fun refreshBoard() {
        paintBrush.clearRect(0.0,0.0, canvas.width, canvas.height)
        renderBaseSquares()
        renderSquareVisuals()
        renderPieces()

    }

    fun renderPieces() {
        val pieces = vc.game.board.fetchPieces()
        var s = 0;

        for (square in 0.until(pieces.size)) {
            val position = convertIntToPairSquare(square)
           // val x = position.first
           // val y = position.second
            if (square != clickedSquare) {
                drawPieceAt(vc.game.board.fetchPiece(square), position)
            }
            s++
        }
            drawPieceAtMouse(vc.game.board.fetchPiece(clickedSquare)) // for layering

    }

    fun renderSquareVisuals() {
        if (game.isOngoing()) {
            vc.updateVisuals()
        for (s in 0.until(BOARD_SIZE)) {
            val row = getRow(s)
            val col = getCol(s)
            val square = Pair(col, row )


                // draw mask
                if (s in vc.pieceMask) {
                    paintBrush.fill = Color.web("#CC3336", 0.47)
                    drawSquare(square)
                }

                // draw checked king
                if (s == vc.checkedKingMask) {
                    paintBrush.fill = Color.web("#d10000", 0.47)
                    drawSquare(square)
                }

                // draw last move
                if (s == vc.lastMoveMask.first || s == vc.lastMoveMask.second) {
                    paintBrush.fill = Color.web("#F5E000", 0.31)
                    drawSquare(square) //
                }
                if (vc.attackSquareMask[s] > 0) {
                    // for (occurence in 0..visualizer.heatMask[s]) {
                    paintBrush.fill = Color.web("d10000", 0.05 * vc.attackSquareMask[s])
                    drawSquare(square) //
                    // }
                }

                // draw move squares of piece
                if ((1L shl s) and vc.validMovesMask != 0L) {
                    paintBrush.fill = Color.web("#7DAFB5", 0.61)
                    drawSquare(square) //
                }

                // draw square numbers // names
                if (vc.showSquareStrings) {
                    val rotatedSquare = adjustForOrientation(convertIntToPairSquare(s))
                    // paintBrush.fill = Color.web("#F44336")
                    paintBrush.fill = when ((col + row) % 2) {
                        0 -> Color.web("#D5AB6D")
                        else -> Color.web("#E9D4B4")
                    }
                    paintBrush.font = Font(20.0)
                    val newpos = adjustForOrientation(Pair(col, row))
                    paintBrush.fillText(
                        getSquareName(s),
                        (newpos.first * SQUARE_DIMENSION + 0.5 * SQUARE_DIMENSION) - 10.0,
                        ((newpos.second * SQUARE_DIMENSION + 0.5 * SQUARE_DIMENSION)) + 10.0
                    )
                    // paintBrush.fillText("$s", ((col * .BoardUtils.SQUARE_DIMENSION + 0.5 * .BoardUtils.SQUARE_DIMENSION) - 10.0), ((row * .BoardUtils.SQUARE_DIMENSION + 0.5 * .BoardUtils.SQUARE_DIMENSION)) + 10.0)

                }
            }
        }

    }

    fun renderBaseSquares() {
        for (s in 0..63) {
            val square = convertIntToPairSquare(s)// adjustForOrientation()
            val col = square.first
            val row = square.second


            // Set square colors
            paintBrush.fill = when ((col + row) % 2) {
                0 ->  Color.web("#E9D4B4")
                else ->  Color.web("#D5AB6D")
            }
            drawSquare(square)
        }
    }

    fun drawPieceAtMouse(piece: Piece) {
       if (!piece.isEmpty()) {
           val image = getImage(piece)
           paintBrush.
           drawImage(
               image,
               mouseX,
               mouseY,
               SQUARE_DIMENSION,
               SQUARE_DIMENSION
           )
       }


   }

    fun drawPieceAt(piece: Piece, position: Pair<Int, Int>) {
        val adjusted = adjustForOrientation(position) // adjustForOrientation()
        if (!piece.isEmpty()) {
            val image = getImage(piece)
                paintBrush.
                drawImage(
                    image,
                    scaleNumberToScreen(adjusted.first),
                    scaleNumberToScreen(adjusted.second),
                    SQUARE_DIMENSION,
                    SQUARE_DIMENSION
                )
        }

    }

    fun getImage(piece: Piece): Image {
        return Image(when (piece.color){
            0    -> imageIndexW[piece.type]
            else -> imageIndexB[piece.type]
        })
    }

    fun drawSquare(position: Pair<Int, Int>) {
        val adjusted = adjustForOrientation(position)
        paintBrush.fillRect(
            adjusted.first * SQUARE_DIMENSION,
            adjusted.second * SQUARE_DIMENSION,
            SQUARE_DIMENSION,
            SQUARE_DIMENSION
        )
    }

    fun adjustForOrientation(coords: Pair<Int,Int>): Pair<Int, Int> {
        return if (vc.orientation == WHITE) Pair(7 - coords.first, 7 - coords.second) else coords
    }

    fun getSquareFromPixels(x: Double, y: Double): Pair<Int, Int> {
        val offset = 9
        squarePositions.forEach { row ->
            row.forEach {
                if (x >= it.first + offset && x < it.first + SQUARE_DIMENSION - offset &&
                    y >= it.second + offset && y < it.second + SQUARE_DIMENSION - offset) {
                    return Pair(
                        7 - (it.first / SQUARE_DIMENSION).toInt(),
                        (it.second / SQUARE_DIMENSION).toInt())
                }
            }
        }
        return Pair(9, 9)
    }

    fun scaleNumberToScreen(number: Int): Double {
        return number.toDouble() * SQUARE_DIMENSION
    }


    fun setSquarePositions() {
        squarePositions.forEachIndexed { row, array -> array.indices.forEach { col ->
            squarePositions[row][col] = Pair(col * SQUARE_DIMENSION, row * SQUARE_DIMENSION) } }
    }

    override fun start(stage: Stage) {
        val root: Parent = FXMLLoader.load(javaClass.getResource("Menu2.fxml"))
        val scene = Scene(root)
        stage.title = "Chess Engine"
        stage.isResizable = false
        stage.scene = scene
        stage.show()
    }
}

fun main() {
    Application.launch(GUI::class.java)
}