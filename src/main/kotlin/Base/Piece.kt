package Base
import BoardUtils.*

@JvmInline
value class Piece(val bitCode: UInt = 0u) {

    // the lsb of each value needs to be in first place value -> 0 0 000    // 000000 // hasmoved, color, type
    val color get() = getProperty(COLOR_SELECTOR, 3)
    val type get() = getProperty(TYPE_SELECTOR, 0)
    val hasMoved get() = getProperty(MOVED_SELECTOR, 4) == 1


    // do pieces get a few extra bits to store their bitboard?

    fun isEmpty(): Boolean = type == EMPTY
    fun isOccupied(): Boolean = type != EMPTY
    fun isPawn(): Boolean = type == PAWN
    fun isKnight(): Boolean = type == KNIGHT
    fun isBishop(): Boolean = type == BISHOP
    fun isRook(): Boolean = type == ROOK
    fun isQueen(): Boolean = type == QUEEN
    fun isKing(): Boolean = type == KING
    fun isLeaper(): Boolean = isKing() || isKnight()
    fun isSlider(): Boolean = isBishop() || isRook() || isQueen()
    fun isLinePiece(): Boolean = isQueen() || isRook()
    fun isDiagonalPiece(): Boolean = isQueen() || isBishop()

    fun isTeamedWith(other: Piece): Boolean {
        if ((this.isEmpty() || other.isEmpty())) {
            return false
        }
        return (this.color == other.color)
    }

    fun isEnemyOf(other: Piece): Boolean {
        if ((this.isEmpty() || other.isEmpty())) {
            return false
        }
        return (this.color != other.color)
    }

    fun fetchColor(): Int {
        return if (isEmpty()) {
            NO_COLOR
        } else color
    }

    fun isColor(color: Int): Boolean = if (isEmpty()) false else this.color == color

    fun moveThisPiece(): Piece = if (hasMoved) this else Piece(bitCode xor MOVED_SELECTOR)

    fun promoteTo(type: Int): Piece {
        require(isPawn() && isPromoteOption(type)) {"This piece is ${typeNames[this.type]}, and cannot promote to ${typeNames[type]}."}
        return Piece(pieceCode(color, type) xor MOVED_SELECTOR)
    }

    private fun isPromoteOption(type: Int): Boolean = when (type) {
        QUEEN, KNIGHT, ROOK, BISHOP -> true
        else -> false
    }
    private fun getProperty(selector: UInt, shift: Int): Int = ((bitCode and selector) shr shift).toInt()




    fun symbol(): String {
        return when (fetchColor()) {
            WHITE -> when (type) {
                PAWN -> "P"
                KNIGHT -> "N"
                BISHOP -> "B"
                ROOK -> "R"
                QUEEN -> "Q"
                KING -> "K"
                else -> " "
            }
            BLACK -> when (type) {
                PAWN -> "p"
                KNIGHT -> "n"
                BISHOP -> "b"
                ROOK -> "r"
                QUEEN -> "q"
                KING -> "k"
                else -> " "
            } else -> " "
        }
    }
    override fun toString(): String {
        return getPieceName(color, type)
    }
}