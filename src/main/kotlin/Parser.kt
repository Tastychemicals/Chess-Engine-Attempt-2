import javafx.scene.control.TextArea
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

class Parser {
    /**
     * ------- Class used explicitly by the GUI --------
     */
    companion object {
        // Operations
        val boardOperations = arrayOf(
            "p",
            "r",
            "c",
            "m",
            "rand" // temporary
        )
        val maskOperations = arrayOf(
            "p",
            "c",
            "m",
            "w",
            "b"

        )
        val settingsOperations = arrayOf(
            "orient",
            "turn",
            "new",
            "squares",
            "l"
        )

        // Parameters
        val pieceTypes = mapOf(
            'p' to BoardHelper.PAWN,
            'n' to BoardHelper.KNIGHT,
            'b' to BoardHelper.BISHOP,
            'r' to BoardHelper.ROOK,
            'q' to BoardHelper.QUEEN,
            'k' to BoardHelper.KING,
        ) // types
        val pieceColors = mapOf(
            'w' to 0,
            'b' to 1
        ) // colors

        val files = mapOf(
            "a" to 0,
            "b" to 1,
            "c" to 2,
            "d" to 3,
            "e" to 4,
            "f" to 5,
            "g" to 6,
            "h" to 7

        ) // positions
        val ranks = mapOf(
            "1" to 7,
            "2" to 6,
            "3" to 5,
            "4" to 4,
            "5" to 3,
            "6" to 2,
            "7" to 1,
            "8" to 0,

        )

        val NO_PARAM = 0
        val NO_LOG = ""

    }
    // board flags
    var pieceMoved = false
    var pieceRemoved = false
    var piecePlaced = false
    var randomPlaced = false
    var boardCleared = false

    // mask flags
    var pieceMaskAdded = false
    var allMaskCleared = false
    var colorMaskAdded = false

    //settings flags
    var orientationChanged = false
    var turnChanged = false
    var newGameStarted = false
    var squareNumsShowing = false



    var commandNotFound = false


    val reference: Game
    val visualizer: Visualizer
    var logs: TextArea?

    var keyComments = arrayOf<String>()

    constructor(reference: Game, visualizer: Visualizer) {
        this.reference = reference
        this.visualizer = visualizer
        this.logs = null
    }


    //todo: fen loader
    fun setLogger(logger: TextArea) {
        this.logs = logger
    }
        // NOTE:

    // the way it returns is still janky. many error messages although command was successful
    fun parseCommand(text: String): String {
        val keyWords = text.split(" ")
        clearLogFlags()
        commandNotFound = false
        when (keyWords[0]) {
            "board" -> beginParseForBoard(restOfString(keyWords))
            "mask" -> parseForMask(restOfString(keyWords))
            "settings" -> parseForSettings(restOfString(keyWords))
            else -> {
                log ("Unknown command: $text")
                commandNotFound = true

            }
        }
        log(isCommandFail(keyWords[1]))
        keyComments += "$text\n"
        keyComments.reverse()
        logs?.text += keyComments.joinToString(" ")
        return keyComments.joinToString(" ")
    }
        //each opener parsing function handles 2 parameters at a time (beginning text plus an additional)
    fun beginParseForBoard(text: List<String>) {
        if (text[0] in boardOperations && text.size < 5) {

            val position1Param = if (text.size > 1) BoardHelper.convertPairToIntSquare(parsePosition(text[1])) else -1
            val randomPiece = BoardHelper.getRandomPiece()
            val randomPosition = Random.nextInt(BOARD_SIZE)


            when (text[0]) {
                "p" -> boardParseTwoParameters(text, NO_PARAM)
                "m" -> boardParseTwoParameters(text, position1Param)
                "r", -> pieceRemoved = reference.board.removePiece(position1Param)
                "c", -> boardCleared = reference.board.clearBoard()
                "rand" -> randomPlaced = reference.board.addPiece(randomPiece, randomPosition) // this is temporary
                else -> commandNotFound = true
            }

            log(
                when {
                    pieceRemoved -> ("Emptied square: ${text[1]}")
                    boardCleared -> ("Board cleared")
                    randomPlaced -> ("Placed randomly: " +
                            BoardHelper.getPieceName(randomPiece.color, randomPiece.type) + "\n" +
                            "at ${BoardHelper.convertIntToPairSquare(randomPosition)}")

                    else -> isCommandFail(text[0])
                }
            )

        }

    }

    fun boardParseTwoParameters(text: List<String>, passedParam: Int ) {
        val typeParam = if (text.size > 1)  parseType(text[1]) else -1
        val colorParam = if (text.size > 1)  parseColor(text[1]) else -1
        val position2param = if (text.size > 2) BoardHelper.convertPairToIntSquare(parsePosition(text[2])) else -1

        when (text[0]) {
            "p" -> piecePlaced = reference.board.addPiece(colorParam, typeParam, position2param)
            "m" -> pieceMoved = reference.board.makeMove(passedParam, position2param)
            else -> commandNotFound = true // this should never be reached
        }

        keyComments += when {
            piecePlaced -> if (typeParam != -1) {("Placed ${BoardHelper.typeNames[typeParam]} at ${text[2]}")} else ""
            pieceMoved -> ("Moved piece from ${text[1]} to ${text[2]}\n" +
                    "actual: ${parsePosition(text[1])} -> ${parsePosition(text[2])}" )
            else -> isCommandFail(text[0])
        }

    }

    fun parseForMask(text: List<String>) {
        if (text[0] in maskOperations && text.size < 4) {
            val pieceParam = if (text.size > 1)  parseType(text[1]) else -1
            val colorParam = if (text.size == 1)  parseColor(text[0]) else -1
            val color2Param =  if (text.size > 1) parseColor(text[1]) else -1



                //todo: Fix parser. It is too janky and feels boxed in. have to refactor
            // NOTE:

            when (text[0]) {
                "p" -> pieceMaskAdded = visualizer.addNewPieceMask(pieceParam, color2Param)
                "c" -> allMaskCleared = visualizer.clearAllMasks()
                "w" -> colorMaskAdded = visualizer.addNewColorMask(colorParam)
                "b" -> colorMaskAdded = visualizer.addNewColorMask(colorParam)
                else -> commandNotFound = true
            }
            log(
                when {
                    pieceMaskAdded -> ("Added mask: ${BoardHelper.getPieceName(color2Param, pieceParam)}\n")
                    colorMaskAdded -> ("Added mask: ${BoardHelper.colorsNames[colorParam]}")
                    allMaskCleared -> ("All masks cleared ")
                    else -> isCommandFail(text[0])
                }
            )



        }
    }

    fun parseForSettings(text: List<String>) {
        if (text[0] in settingsOperations && text.size < 8) {
            val color = if (text.size > 1) parseColor(text[1]) else -1
            val fen = if (text.size > 1) text[1] else NO_PARAM.toString()
            when (text[0]){
                "turn" -> turnChanged = reference.changeTurn(color)
                "orient" ->  orientationChanged = visualizer.setNewOrientation(color)
                "squares" -> squareNumsShowing = visualizer.setShowingSquares()
                "new" ->   newGameStarted = reference.startNewGame()
                "l" -> if (fen != NO_PARAM.toString()) reference.startNewGame(fen)
                else -> commandNotFound = true
            }

            log(
                when {
                    turnChanged -> ("Turn changed to: ${BoardHelper.colors[color]}")
                    orientationChanged -> ("Orientation: ${BoardHelper.colors[color]}")
                    squareNumsShowing -> ("Toggled square numbers")
                    else -> isCommandFail(text[0])
                }
            )
        }
    }

    fun parseType(text: String): Int {
        val type = text.toCharArray().last()
        if (type in pieceTypes.keys) {
            return pieceTypes[type] as Int
        } else {
                log ("\n->   piece/color $text could not be parsed.")
                return  -1
        }
    }

    fun parsePosition(text: String): Pair<Int,Int> {
        val literals = text.toCharArray().map { it.toString() }.filter { it in files.keys || it in ranks.keys}
        if (literals.size > 1) {
            if (files[literals[0]] != null && ranks[literals[1]] != null) {
                return Pair(files[literals[0]]!!, ranks[literals[1]]!!)
            } else {
                log("\n->   Position ${literals[0]},${literals[1]} could not be found.")
            }
        }
        return Pair(-1,-1)
    }

    fun parseColor(text: String): Int {
        val color = text.toCharArray()[0]
        return if (color in pieceColors.keys) {
            pieceColors[color] as Int
        } else {
            log ("\n->   Color $text does not exist.")
            -1
        }
    }


    fun restOfString(text: List<String>): List<String> {
        return text.slice(1.until(text.size))
    }

    fun isCommandFail(text: String): String {
        return if (commandNotFound) { ("command ${text[0]} is incomplete or unresolvable.") } else NO_LOG
    }

    fun log(text: String) {
        if (text != NO_LOG) {
            keyComments += ("$text\n")
        }
    }

    fun clearLogFlags() {
        keyComments = emptyArray()

        // board flags
         pieceMoved = false
         pieceRemoved = false
         piecePlaced = false
         randomPlaced = false
         boardCleared = false

        // mask flags
         pieceMaskAdded = false
         allMaskCleared = false
         colorMaskAdded = false

        //settings flags
         orientationChanged = false
         turnChanged = false
         newGameStarted = false
         squareNumsShowing = false

         commandNotFound = false
    }


}