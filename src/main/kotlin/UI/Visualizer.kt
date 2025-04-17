package UI

import BoardUtils.BOARD_SIZE
import Game.Game
import Game.Piece

class Visualizer {
    /**
     * ------- Class used explicitly by the .GUI --------
     */
    var lastMoveMask: Pair<Int,Int>

    var typeFilter: MutableSet<Int>
    var colorFilter: MutableSet<Int>
    var positionFilter: MutableSet<Int>
    var Filters = mutableSetOf< (Piece) -> Boolean >()

    var mask = mutableListOf<Int>()
    var moveSquareMask = mutableSetOf<Int>()
    var checkedKingMask: Int = -1
    var heatMask = mutableMapOf<Int, Int>()

    var showAllSquares: Boolean
    var emptySquareFiltering = true

    var orientation = 0 // 0 = White POV, 1 = Black POV
    val display: Game

    constructor(display: Game) {
        this.display = display


        this.typeFilter = mutableSetOf<Int>()
        this.colorFilter = mutableSetOf<Int>()
        this.positionFilter = mutableSetOf<Int>()

        this.showAllSquares = true
        this.orientation = 0
        this.lastMoveMask = display.board.lastMove

    }

//todo: last move highlight mask
    fun addNewTypeMask(type: Int) {
        Filters.add {  it.type == type }
    }

    fun addNewPieceMask(type: Int, color: Int): Boolean {
        Filters.add { !it.isEmpty() && it.type == type && it.color == color }
        updateCustomMask()
        return true
    }

    fun addNewColorMask(pieceColor: Int): Boolean {
         if (pieceColor != -1) {
            Filters.add { !it.isEmpty() && it.color == pieceColor }
            return true
         }
        return false
    }

    fun updateCustomMask(): Boolean {
        mask.clear()
        moveSquareMask.clear()
        heatMask.clear()
        checkedKingMask = display.board.moveGenerator.getCheckedKing(display.turn)
        heatMask = display.board.moveGenerator.enemyAttackSquares
        val pieces = display.board.fetchAllPieces()

        for (square in 0.until(BOARD_SIZE)) {
            for (filter in Filters) {
                if ( filter(pieces[square]) ) {
                    mask.add(square)
                     break // they meet at least 1 requirement
                }
            }
        }
        lastMoveMask = display.board.lastMove

        return true
        println("mask squares:" + mask)
    }

    fun addMoveSquareMask(square: Int): Boolean {
        moveSquareMask = display.board.moveGenerator.genAllLegalMoves(display.board.fetchPiece(square).color)[square] ?: mutableSetOf()
        //moveSquareMask = display.board.moveGenerator.crawlMoves(square, display.board.fetchPiece(square), display.board.moveGenerator.kingMoveInstructions).toMutableSet()
        return true
    }

    fun clearCustomMask() {

        updateCustomMask()
    }
    fun clearAllMasks(): Boolean {
        typeFilter.clear()
        colorFilter.clear()
        positionFilter.clear()
        Filters.clear()
        lastMoveMask = Pair(-1,-1)
        return true
    }

        //todo: need to add better command parsing to allow manipulation of moveMasks and lastMove masks
    fun setNewOrientation(orientation: Int): Boolean {
       if (orientation != -1) {
         //  println("old: " + "$orientation" + " (inside visualizer setNewOrientation)")
            this.orientation = orientation
          // println("new: " + "$orientation" + " (inside visualizer setNewOrientation)")
            return true
       }
            return false
    }

    fun setShowingSquares(): Boolean {
        showAllSquares = showAllSquares != true
        return showAllSquares
    }
    fun setShowMaskSquares() {

    }
}