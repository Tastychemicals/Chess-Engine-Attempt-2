package BoardUtils
import Game.Piece
import UI.Parser.Companion.pieceColors
import UI.Parser.Companion.pieceTypes
import kotlin.math.abs
import kotlin.random.Random
typealias move = Int
// ---------- Board ----------
const val BOARD_SIZE = 64
const val BOARD_WIDTH = BOARD_SIZE / 8
const val SQUARE_DIMENSION = 67.5
// -----------------------------



// ---------- Pieces ----------

const val EMPTY = 0
const val PAWN = 1;
const val KNIGHT = 2;
const val BISHOP = 3;
const val ROOK = 4;
const val QUEEN = 5;
const val KING = 6;

const val WHITE = 0
const val BLACK = 1
const val NO_COLOR = -1

const val MOVED_SELECTOR: UInt =    0b0000010000u
const val COLOR_SELECTOR: UInt =    0b0000001000u
const val TYPE_SELECTOR: UInt =     0b0000000111u

const val CASTLE_MOVE_DISTANCE = 2

val typeNames = arrayOf(
    "EMPTY",
    "PAWN",
    "KNIGHT",
    "BISHOP",
    "ROOK",
    "QUEEN",
    "KING",
)

fun pieceCode(color: Int, type: Int): UInt {
    val bitCode = 0u shl 1 or                   // hasMoved : 4 bits deep
            convertToBinary(color) shl 3 or     // color    : 3 bits deep
            convertToBinary(type)               // type     : 1 bit deep
    return bitCode
}
// -----------------------------

// ---------- Moves ----------
object Move {
    fun encode(startSquare: Int, endSquare: Int, flags: Int = 0): move {
        return  (startSquare shl START_SQUARE_BIT) or (endSquare shl END_SQUARE_BIT) or  (flags shl FLAGS_BIT)

    }
    fun getStart(move: move): Int {
        return (move and START_SQUARE_SELECTOR) shr START_SQUARE_BIT
    }
    fun getEnd(move: move): Int {
        return (move and END_SQUARE_SELECTOR) shr END_SQUARE_BIT
    }
    fun getFlags(move: move): Int {
        return (move and FLAGS_SELECTOR) shr FLAGS_BIT
    }
    fun encodeFlags(
        capture: Boolean = false,
        castle: Boolean = false,
        check: Boolean = false,
        checkmate: Boolean = false,
        promotion: Boolean = false,
        enpassant: Boolean = false
    ): Int {
        return ((if (capture) 1 else 0) shl CAPTURE_BIT) or
                ((if (castle) 1 else 0) shl CASTLE_BIT) or
                ((if (check) 1 else 0) shl CHECK_BIT) or
                ((if (checkmate) 1 else 0) shl CHECKMATE_BIT) or
                ((if (promotion) 1 else 0) shl PROMOTION_BIT) or
                ((if (enpassant) 1 else 0) shl ENPASSANT_BIT)
    }
    fun isCapture (move: move): Boolean {
        return getFlags(move) and CAPTURE_FLAG != 0
    }
    fun isCastle (move: move): Boolean {
        return getFlags(move) and CASTLE_FLAG != 0
    }
    fun isCheck (move: move): Boolean {
        return getFlags(move) and CHECK_FLAG != 0
    }
    fun isCheckMate (move: move): Boolean {
        return getFlags(move) and CHECKMATE_FLAG != 0
    }
    fun isPromotion (move: move): Boolean {
        return getFlags(move) and PROMOTION_FLAG != 0
    }
    fun isEnPassant(move: move): Boolean {
        return getFlags(move) and ENPASSANT_FLAG != 0
    }
    fun isQuiet(move: move): Boolean {
        return getFlags(move) and (CAPTURE_FLAG or PROMOTION_FLAG or CASTLE_FLAG or ENPASSANT_FLAG) == 0
    }

    private const val START_SQUARE_BIT = 0
    private const val END_SQUARE_BIT = 6
    private const val FLAGS_BIT = 12

    private const val START_SQUARE_SELECTOR =   0b000000000000111111
    private const val END_SQUARE_SELECTOR =     0b000000111111000000
    private const val FLAGS_SELECTOR =          0b111111000000000000

    // 000000 000000 000000

    private const val CAPTURE_BIT = 0
    private const val CASTLE_BIT = 1
    private const val CHECK_BIT = 2
    private const val CHECKMATE_BIT = 3
    private const val PROMOTION_BIT = 4
    private const val ENPASSANT_BIT = 5

    private const val CAPTURE_FLAG =    1 shl CAPTURE_BIT         // 0b0000000001
    private const val CASTLE_FLAG =     1 shl CASTLE_BIT          // 0b0000000010
    private const val CHECK_FLAG =      1 shl CHECK_BIT           // 0b0000000100
    private const val CHECKMATE_FLAG =  1 shl CHECKMATE_BIT       // 0b0000001000
    private const val PROMOTION_FLAG =  1 shl PROMOTION_BIT       // 0b0000010000
    private const val ENPASSANT_FLAG =  1 shl ENPASSANT_BIT       // 0b0000100000

}






fun convertPairToIntSquare(square: Pair<Int,Int>): Int {
    val x = square.first
    val y = square.second
    if (x == 0 && y == 0) return 0
    return if (x in 0..8 && y in 0..8) (y * 8 + x) else -1
}

fun convertIntToPairSquare(squareNumber: Int): Pair<Int, Int> {
    val x = (squareNumber % 8)
    val y = squareNumber / 8
    return  Pair(x, y)
}

fun colDistance(origin: Int, endSquare: Int): Int =
    abs(convertIntToPairSquare(origin).first - convertIntToPairSquare(endSquare).first)
fun rowDistance(origin: Int, endSquare: Int): Int =
    abs(convertIntToPairSquare(origin).second - convertIntToPairSquare(endSquare).second)

fun isOnDiffRow(origin: Int, endSquare: Int): Boolean {
    return rowDistance(origin, endSquare) != 0
}
fun isOnDiffCol(origin: Int, endSquare: Int): Boolean {
    return colDistance(origin, endSquare) != 0
}

fun isOnSide(square: Int): Boolean = (square % 8 == 0 || square % 8 == 7)
fun isOnBack(square: Int): Boolean {
    val yPos = convertIntToPairSquare(square).second
    return yPos % 8 == 0 || yPos % 8 == 7
}

fun doesWrap(origin: Int, endSquare: Int): Boolean = ((colDistance(origin, endSquare) > 2))
fun doesNotWrap(origin: Int, endSquare: Int): Boolean = (colDistance(origin, endSquare) <= 2)

fun getSquareBehind(squarePosition: Int, color: Int): Int {
    return squarePosition + (8 * getPawnDirection(getOppositeColor(color)))
}


fun getOppositeColor(color: Int): Int {
   return when (color) {
        0 -> 1
        else -> 0
    }
}

fun getPawnDirection(color: Int): Int = when (color) {
     BLACK -> 1
     WHITE -> -1
     else -> 0
}




fun convertToBinary(number: Int): UInt {

    var n = number
    var r = 0.0 // remainder
    var wholeBit = 0u
    var i = 0

    while (n > 0) {

        r = (n % 2.0)
        n = n/2

        wholeBit = (wholeBit or ((1u shl i) and ((r.toUInt() shl i))) )
        i++
    }

//        println("-------------")
//        println("number: $number\n" +
//                     "got: ${wholeBit.toInt().toString(2).padStart(10, '0')} \n" +
//                     "decimal: ${wholeBit.toInt()}" +
//                "\npiece: ${colors[Game.Piece(wholeBit).color]} ${allPieceNames[Game.Piece(wholeBit).type]} ${BoardUtils.convertIntToPairSquare(Game.Piece(wholeBit).position)}" +
//            "\npiece: ${Game.Piece(wholeBit).color} ${Game.Piece(wholeBit).type} ${Game.Piece(wholeBit).position}")
//            println("piece code: ${getPieceCode()}")

    // println("-------------")
    return wholeBit
}

fun getPieceFromFen(letter: Char): Piece {
    return when (letter) { // todo: rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
        'r' -> Piece(pieceCode(BLACK, ROOK))
        'n' -> Piece(pieceCode(BLACK, KNIGHT))
        'b' -> Piece(pieceCode(BLACK, BISHOP))
        'q' -> Piece(pieceCode(BLACK, QUEEN))
        'k' -> Piece(pieceCode(BLACK, KING))
        'p' -> Piece(pieceCode(BLACK, PAWN))
        'R' -> Piece(pieceCode(WHITE, ROOK))
        'N' -> Piece(pieceCode(WHITE, KNIGHT))
        'B' -> Piece(pieceCode(WHITE, BISHOP))
        'Q' -> Piece(pieceCode(WHITE, QUEEN))
        'K' -> Piece(pieceCode(WHITE, KING))
        'P' -> Piece(pieceCode(WHITE, PAWN))
        else -> Piece(pieceCode(WHITE, EMPTY))
    }
}

fun getRandomPiece(): Piece {
    return Piece(
        pieceCode(
            Random.nextInt(pieceColors.size),
            Random.nextInt(1, pieceTypes.size)
        ) // for excluding empty squares)
    )
}



val colorsNames = arrayOf(
    "WHITE",
    "BLACK"
)

fun simplifyFenBoard(fenString: String): String {
    var fenRebuilder = StringBuilder()
    val fenBoard = fenString.split(" ")[0].split("/")
    for (rank in 0..7) {
        val fenRank = fenBoard[rank]
        for (char in fenRank) {
            if (char.isDigit()) {
                fenRebuilder.append("0".padStart(char.digitToInt(), '0'), )
            } else {
                fenRebuilder.append(char)
            }
        }
        // fenRebuilder.append("/")
    }
    // println(fenRebuilder)
    return fenRebuilder.toString()

}


fun getPieceName(color: Int, type: Int): String {
    return "${colorsNames[color]}  ${typeNames[type]}"
}




class BoardHelper {


    companion object {

        private val magicHex = arrayOf(
            0x5555555555555555UL, 0x3333333333333333UL, 0x0F0F0F0F0F0F0F0FUL, 0x0101010101010101uL
        )



        // 0 000 000000  -> color bit, type bits, position bits

       // const val POSITION_SELECTOR: UInt = 0b0000111111u


        val colors = arrayOf(
            WHITE,
            BLACK
        )
        val pieces = arrayOf(
            EMPTY,
            PAWN,
            KNIGHT,
            BISHOP,
            ROOK,
            QUEEN,
            KING,

            )




        fun countPieces(p: ULong): Int {

            var c = p
            c = c - ((c shr 1) and magicHex[0])
            c = (c and magicHex[1]) + ((c shr 2) and magicHex[1])
            c = (c + (c shr 4)) and magicHex[2]
            c *= magicHex[3]

            return (c shr 56).toInt()
        }


        /**
         * @return the [squarePosition] as a Bit Game.Board.
         */
        fun convertIntSquareToBit(squarePosition: Int): ULong {
            return if (squarePosition != -1){
                1uL shl squarePosition
            } else 0uL
        }




    }
}