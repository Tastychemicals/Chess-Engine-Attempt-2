package UI

import Base.Board
import Base.MoveGenerator
import BoardUtils.BOARD_SIZE
import Base.Piece

class Visualizer {
    /**
     * ------- Class used explicitly by the GUI --------
     */
    enum class Operation {
        CLEAR_ALL,
        ADD_PIECE_MASK,


    }

    var lastMoveMask = Pair(-1,-1)

    private val pieceFilters = mutableSetOf< (Piece) -> Boolean >()
    var pieceMask = mutableListOf<Int>()
    var validMovesMask = 0L
    var checkedKingMask: Int = -1
    var attackSquareMask = IntArray(64)
    var showSquareStrings = true

    var orientation = 0 // 0 = White POV, 1 = Black POV
    private var display = Board()
    private val mg = MoveGenerator(Board())

    fun setBoard(board: Board) {
        display = board
        mg.setReferenceBoard(board)
    }


    fun clearAllMasks(): Boolean {
        pieceFilters.clear()
        lastMoveMask = Pair(-1,-1)
        validMovesMask = 0L
        attackSquareMask = IntArray(64)
        pieceMask.clear()
        checkedKingMask = -1
        return true
    }

    //todo: last move highlight mask

    fun addPieceMask(type: Int? = null, color: Int? = null): Boolean {
        if (type == null && color == null) return false
        if (type == null && color != null)  {
            pieceFilters.add { it.isColor(color); }
        }
        if (type != null && color == null) {
            pieceFilters.add { it.type == type }
        }
        if (type != null && color != null) {
            pieceFilters.add { it.type == type && it.isColor(color) }
        }
        return true
    }

    fun addMoveSquareMasks(square: Int): Boolean {
        validMovesMask =  mg.genLegalPieceMoves(square)
        return true
    }

    fun update(): Boolean {
        checkedKingMask = mg.getCheckedKing()
        attackSquareMask = mg.enemyAttackSquares
        val pieces = display.fetchPieces()

        for (square in 0.until(BOARD_SIZE)) {
            for (filter in pieceFilters) {
                if ( filter(pieces[square]) ) {
                    pieceMask.add(square)
                     break // they meet at least 1 requirement
                }
            }
        }
        lastMoveMask = display.lastMove?: Pair(-1,-1)

        return true
        // println("mask squares: $pieceMask")
    }

    //todo: need to add better command parsing to allow manipulation of moveMasks and lastMove masks
    fun setNewOrientation(orientation: Int): Boolean {
       if (orientation != -1) {
            this.orientation = orientation

            return true
       }
            return false
    }

    fun setShowingSquares(): Boolean {
        showSquareStrings = showSquareStrings != true
        return showSquareStrings
    }
    fun setShowMaskSquares() {

    }
}