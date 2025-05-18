package UI

import BoardUtils.BISHOP
import BoardUtils.BOARD_SIZE
import Base.Game
import BoardUtils.*
import BoardUtils.convertPairToIntSquare
import javafx.scene.control.TextArea
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
            "l",
            "s",
            "d",
            "kill"
        )

        // Parameters
        val pieceTypes = mapOf(
            'p' to PAWN,
            'n' to KNIGHT,
            'b' to BISHOP,
            'r' to ROOK,
            'q' to QUEEN,
            'k' to KING,
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
            "8" to 7,
            "7" to 6,
            "6" to 5,
            "5" to 4,
            "4" to 3,
            "3" to 2,
            "2" to 1,
            "1" to 0,

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

            val position1Param = if (text.size > 1) convertPairToIntSquare(parsePosition(text[1])) else -1
            val randomPiece = getRandomPiece()
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
                    boardCleared -> ("Game.Board cleared")
                    randomPlaced -> ("Placed randomly: " +
                            getPieceName(randomPiece.color, randomPiece.type) + "\n" +
                            "at ${convertIntToPairSquare(randomPosition)}")

                    else -> isCommandFail(text[0])
                }
            )

        }

    }

    fun boardParseTwoParameters(text: List<String>, passedParam: Int ) {
        val typeParam = if (text.size > 1)  parseType(text[1]) else -1
        val colorParam = if (text.size > 1)  parseColor(text[1]) else -1
        val position2param = if (text.size > 2) convertPairToIntSquare(parsePosition(text[2])) else -1

        when (text[0]) {
            "p" -> piecePlaced = reference.board.addPiece(colorParam, typeParam, position2param)
            "m" -> pieceMoved = reference.board.makeMove(passedParam, position2param)
            else -> commandNotFound = true // this should never be reached
        }

        keyComments += when {
            piecePlaced -> if (typeParam != -1) {("Placed ${typeNames[typeParam]} at ${text[2]}")} else ""
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
                "p" -> pieceMaskAdded = visualizer.addPieceMask(pieceParam, color2Param)
                "c" -> allMaskCleared = visualizer.clearAllMasks()
                "w" -> colorMaskAdded = visualizer.addPieceMask(color = colorParam)
                "b" -> colorMaskAdded = visualizer.addPieceMask(color = colorParam)
                else -> commandNotFound = true
            }
            log(
                when {
                    pieceMaskAdded -> ("Added mask: ${getPieceName(color2Param, pieceParam)}\n")
                    colorMaskAdded -> ("Added mask: ${colorsNames[colorParam]}")
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
                "orient" ->  orientationChanged = visualizer.setNewOrientation(1 - color)
                "squares" -> squareNumsShowing = visualizer.setShowingSquares()
                "new" ->   { newGameStarted = true ; reference.prepareToBegin() }
                "l" -> if (fen != NO_PARAM.toString()) reference.prepareToBegin(fen)
                "s" -> when (fen) {
                    else -> loadPinsPosition1()
                }
                "d" -> when (fen) {
                    else -> loadPawnsPosition2()
                }
                "kill" -> Game.Sessions.vacateAll()
                else -> commandNotFound = true
            }

            log(
                when {
                    turnChanged -> ("Turn changed to: $color")
                    orientationChanged -> ("Orientation: $color")
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

    fun loadPinsPosition1() {
        //visualizer.setNewOrientation()
        visualizer.setNewOrientation(reference.player1color)
        reference.prepareToBegin()
        reference.board.clearBoard()
        reference.board.addPiece(BLACK, KING, 49)
        reference.board.addPiece(BLACK, ROOK, 33)
        reference.board.addPiece(WHITE, ROOK, 9)
        reference.board.addPiece(WHITE, ROOK, 36)
        reference.board.addPiece(WHITE, KING, 38)
        reference.board.addPiece(BLACK, ROOK, 42)
        reference.board.addPiece(WHITE, QUEEN, 28)
        reference.board.addPiece(BLACK, ROOK, 32)
        reference.board.addPiece(WHITE, PAWN, 59)
        reference.board.addPiece(WHITE, KNIGHT, 24)
    }
    fun loadPawnsPosition2() {
        //visualizer.setNewOrientation()
        visualizer.setNewOrientation(reference.player1color)
        reference.prepareToBegin()
        Thread.sleep(400)
        reference.board.clearBoard()
        reference.board.addPiece(BLACK, PAWN, 15)
        reference.board.addPiece(BLACK, PAWN, 8)
        reference.board.addPiece(BLACK, PAWN, 13)
        reference.board.addPiece(BLACK, PAWN, 12)

        reference.board.addPiece(WHITE, PAWN, 55)
        reference.board.addPiece(WHITE, PAWN, 54)
        reference.board.addPiece(WHITE, PAWN, 53)
        reference.board.addPiece(WHITE, PAWN, 52)
        reference.board.addPiece(WHITE, PAWN, 51)


        reference.board.addPiece(BLACK, KING, 49)
        reference.board.addPiece(WHITE, KING, 38)
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