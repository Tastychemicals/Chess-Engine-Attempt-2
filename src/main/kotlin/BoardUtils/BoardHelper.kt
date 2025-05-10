package BoardUtils
import Base.Piece
import UI.Parser.Companion.pieceColors
import UI.Parser.Companion.pieceTypes
import kotlin.math.abs
import kotlin.random.Random
typealias move = Int
// ---------- Board ----------
const val BOARD_SIZE = 64
const val BOARD_AXIS_LENGTH = BOARD_SIZE / 8
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
val colorsNames = arrayOf(
    "WHITE",
    "BLACK"
)

fun pieceCode(color: Int, type: Int, hasMoved: Boolean = false): UInt {
    val bitCode = (if (hasMoved) 1u else 0u) shl 1 or     // hasMoved : 4 bits deep
            convertToBinary(color) shl 3 or     // color    : 3 bits deep
            convertToBinary(type)               // type     : 1 bit deep
    return bitCode
}

fun getRandomPiece(): Piece {
    return Piece(
        pieceCode(
            Random.nextInt(pieceColors.size),
            Random.nextInt(1, pieceTypes.size)
        ) // for excluding empty squares)
    )
}

fun makePiece(color: Int, type: Int, hasMoved: Boolean = false): Piece {
    return Piece(pieceCode(color, type, hasMoved))
}

fun getPieceName(color: Int, type: Int): String {
    return if (type == EMPTY) ". " else
        "${colorsNames[color]} ${typeNames[type]}"
}

// -----------------------------

// ---------- Board ----------

fun convertPairToIntSquare(square: Pair<Int,Int>): Int {
    val x = square.first
    val y = square.second
    if (x == 0 && y == 0) return 0
    return if (x in 0..8 && y in 0..8) (y * 8 + x) else -1
}
fun convertIntToPairSquare(squareNumber: Int): Pair<Int, Int> {         // might be deprecated, potentially use getRow() or getCol()
    val x = 7 - (squareNumber % 8)
    val y = squareNumber / 8
    return  Pair(x, y)
}

fun getRow(square: Int): Int {
    return square / 8
}

fun getCol(square: Int): Int {
    return 7 - (square % 8)
}

fun rowIs(square: Int, row: Int): Boolean {
    return getRow(square) == row
}

fun colIs(square: Int, col: Int): Boolean {
    return getCol(square) == col
}

fun getPromotionRank(color: Int): Int {
    return when (color) {
        WHITE -> 7
        BLACK -> 0
        else -> -1
    }
}

fun isBeforePromotionRank(square: Int, color: Int): Boolean {
    return rowIs(square, getPromotionRank(color) - getPawnDirection(color))
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

fun isOnSameRow(origin: Int, endSquare: Int): Boolean {
    return rowDistance(origin, endSquare) == 0
}
fun isOnSameCol(origin: Int, endSquare: Int): Boolean {
    return colDistance(origin, endSquare) == 0
}
fun isDiagonalMove(origin: Int, endSquare: Int): Boolean {
    return rowDistance(origin, endSquare) == colDistance(origin, endSquare)
}

fun isOnSide(square: Int): Boolean = (square % 8 == 0 || square % 8 == 7)
fun isOnBack(square: Int): Boolean {
    val yPos = convertIntToPairSquare(square).second
    return yPos == 0 || yPos == 7
}

fun doesWrap(origin: Int, endSquare: Int): Boolean = ((colDistance(origin, endSquare) > 2))
fun doesNotWrap(origin: Int, endSquare: Int): Boolean = (colDistance(origin, endSquare) <= 2)

fun getSquareBehind(squarePosition: Int, color: Int): Int {
    return squarePosition + (8 * getPawnDirection(getOppositeColor(color)))
}
// ---------------------------

fun getOppositeColor(color: Int): Int {
   return when (color) {
        WHITE -> BLACK
        else -> WHITE
    }
}
fun getPawnDirection(color: Int): Int = when (color) {
     BLACK -> -1
     WHITE -> 1
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

fun getSquareName(square: Int): String {
    val coords = convertIntToPairSquare(square)
    return fileNames[coords.first] + rankNames[coords.second]
}


val fileNames = mapOf(
    0 to "h",
    1 to "g",
    2 to "f",
    3 to "e",
    4 to "d",
    5 to "c",
    6 to "b",
    7 to "a"
)

val rankNames = mapOf(
    0 to "1",
    1 to "2",
    2 to "3",
    3 to "4",
    4 to "5",
    5 to "6",
    6 to "7",
    7 to "8"
)


fun simplifyFenBoard(fenString: String): String {
    val fenRebuilder = StringBuilder()
    val fenBoard = fenString.split(" ")[0].split("/").reversed()
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