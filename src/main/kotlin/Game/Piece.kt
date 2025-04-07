package Game
import BoardUtils.*

@JvmInline
value class Piece(val bitCode: UInt) {

    // the lsb of each value needs to be in first place value -> 0 0 000    // 000000 // hasmoved, color, type
    val color get() = getProperty(COLOR_SELECTOR, 3)
    val type get() = getProperty(TYPE_SELECTOR, 0)
    val hasMoved get() = getProperty(MOVED_SELECTOR, 4) == 1


    // do pieces get a few extra bits to store their bitboard?

    fun isEmpty(): Boolean = type == EMPTY
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

    fun moveThisPiece(): Piece = if (hasMoved) this else Piece(bitCode xor MOVED_SELECTOR)

    private fun getProperty(selector: UInt, shift: Int): Int = ((bitCode and selector) shr shift).toInt()





    override fun toString(): String {
        return getPieceName(color, type)
    }
}