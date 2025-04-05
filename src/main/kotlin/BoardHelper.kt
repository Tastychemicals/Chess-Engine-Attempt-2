import Parser.Companion.pieceColors
import Parser.Companion.pieceTypes
import java.lang.Character.isDigit
import kotlin.math.floor
import kotlin.random.Random

const val BOARD_SIZE = 64
const val BOARD_WIDTH = BOARD_SIZE/8
const val SQUARE_DIMENSION = 67.5

const val EMPTY = 0
const val PAWN = 1;
const val KNIGHT = 2;
const val BISHOP = 3;
const val ROOK = 4;
const val QUEEN = 5;
const val KING = 6;

class BoardHelper {


    companion object {

        private val magicHex = arrayOf(
            0x5555555555555555UL, 0x3333333333333333UL, 0x0F0F0F0F0F0F0F0FUL, 0x0101010101010101uL
        )




        const val WHITE = 0
        const val BLACK = 1

        // 0 000 000000  -> color bit, type bits, position bits
        const val COLOR_SELECTOR: UInt =    0b0000001000u
        const val TYPE_SELECTOR: UInt =     0b0000000111u
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
            this::WHITE.name,
            this::BLACK.name
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
            return Piece(pieceCode(
                Random.nextInt(pieceColors.size),
                Random.nextInt(1, pieceTypes.size)) // for excluding empty squares)
            )
        }

        fun getPieceName(color: Int, type: Int): String {
            return "${colorsNames[color]}  ${typeNames[type]}"
        }

        fun countPieces(p: ULong): Int {

            var c = p
            c = c - ((c shr 1) and magicHex[0])
            c = (c and magicHex[1]) + ((c shr 2) and magicHex[1])
            c = (c + (c shr 4)) and magicHex[2]
            c *= magicHex[3]

            return (c shr 56).toInt()
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
        /**
         * @return the [squarePosition] as a Bit Board.
         */
        fun convertIntSquareToBit(squarePosition: Int): ULong {
            return if (squarePosition != -1){
                1uL shl squarePosition
            } else 0uL
        }

        fun pieceCode(color: Int, type: Int): UInt {
            val bitCode = 0u or convertToBinary(color) shl 3 or convertToBinary(type) //or convertToBinary(position)shl 3
           //println(bitCode.toString(2).padStart(4,'0'))
           // println(Piece(bitCode))
            return bitCode
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
//                "\npiece: ${colors[Piece(wholeBit).color]} ${allPieceNames[Piece(wholeBit).type]} ${convertIntToPairSquare(Piece(wholeBit).position)}" +
//            "\npiece: ${Piece(wholeBit).color} ${Piece(wholeBit).type} ${Piece(wholeBit).position}")
//            println("piece code: ${getPieceCode()}")

           // println("-------------")
            return wholeBit
        }


    }
}