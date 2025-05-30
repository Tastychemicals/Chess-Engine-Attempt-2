package Base
import BoardUtils.*

class Board {
    private var enpassantSquare: Int? = null
    private var whiteKingPosition: Int? = null
    private var blackKingPosition: Int? = null

    private var pieces = Array<Piece>(BOARD_SIZE) { EMPTY_SQUARE }
    private var whitePieces = Array<Piece>(BOARD_SIZE) { EMPTY_SQUARE }
    private var blackPieces = Array<Piece>(BOARD_SIZE) { EMPTY_SQUARE }
    private var whitePieceCount = 0;
    private var blackPieceCount = 0;
    private companion object {
        val EMPTY_SQUARE = Piece(pieceCode(0,0))
        const val WHITE_KING_START_SQUARE = 4;
        const val BLACK_KING_START_SQUARE = 60
        const val WHITE_PAWN_START_RANK = 1
        const val BLACK_PAWN_START_RANK = 6
    }

    var lastMove: Pair<Int,Int>? = null
    private var rookDestinationHolder = Holder<Int>()

    constructor()
    private constructor (enpassantSquare: Int?,
                 whiteKingPosition: Int?,
                 blackKingPosition: Int?,
                 pieces: Array<Piece>,
                 lastMove: Pair<Int,Int>?,
                 rookDestination: Holder<Int>
    ) {
        this.enpassantSquare = enpassantSquare
        this.whiteKingPosition = whiteKingPosition
        this.blackKingPosition = blackKingPosition
        copyPieces(pieces)
        this.lastMove = lastMove?.copy()
        this.rookDestinationHolder = rookDestination.clone()
    }

    private fun copyPieces(pieces: Array<Piece>) {
        for (i in 0.until (pieces.size)) {
            val copyPiece = pieces[i].copy()
            this.pieces[i] = copyPiece
            if (copyPiece.isColor(WHITE))  {
                whitePieceCount++
                this.whitePieces[i] = copyPiece
            }
            if (copyPiece.isColor(BLACK)) {
                blackPieceCount++
                this.blackPieces[i] = copyPiece
            }
        }
    }

    fun deepCopy(): Board {
        return Board(enpassantSquare, whiteKingPosition, blackKingPosition, pieces, lastMove, rookDestinationHolder )
    }

    /**
     * Initializes this board's bit boards.
     */
    fun loadBoard(fenString: String) {
        clearBoard()
        val digestibleBoard = simplifyFenBoard(fenString)
        for (square in 0.until(BOARD_SIZE)) {
            val piece = getPieceFromFen(digestibleBoard[square])
            //if (piece.isPawn() && rowIs(square, ))
            pieces[square] = piece
            addToColorArray(piece,square)
            if (piece.isKing()) {
                updateKingPosition(piece.color, square)
            }
        }
    }
    /**
     * Clears the board of its pieces.
     *
     * @return true
     */
    fun clearBoard(): Boolean {
        pieces = Array<Piece>(BOARD_SIZE) { EMPTY_SQUARE }
        whitePieces = Array<Piece>(BOARD_SIZE) { EMPTY_SQUARE }
        blackPieces = Array<Piece>(BOARD_SIZE) { EMPTY_SQUARE }
        whitePieceCount = 0
        blackPieceCount = 0
        return true
    }
    /**
     * Places a Game.Piece on the specified bitboard if the
     * specified position is empty on the board.
     *
     * @return true if a piece was added.
     */
    fun addPiece(piece: Piece, square: Int): Boolean {
        if (isInBounds(square) && !piece.isEmpty()) {
            if (pieces[square].isEmpty()) {

                addToColorArray(piece, square)
                pieces[square] = piece

                if (piece.isKing()) {
                    updateKingPosition(piece.color, square)
                }
                return true
            }
        }
        return false
    }

    fun addPiece(color: Int, type: Int, squarePosition: Int): Boolean {
        if (isInBounds(squarePosition) && type != EMPTY) {
            return addPiece(Piece(pieceCode(color, type)), squarePosition)
        }
        return false
    }

    private fun addToColorArray(piece: Piece, position: Int) {
        if (piece.isColor(WHITE)) {
            whitePieces[position] = piece
            whitePieceCount++
        }
        else {
            if (piece.isColor(BLACK)) {
                blackPieces[position] = piece
                blackPieceCount++
            }

        }

    }

    private fun removeFromColorArrays(position: Int) {
        if (whitePieces[position].isOccupied()) {
            whitePieces[position] = EMPTY_SQUARE
            whitePieceCount--
        }
        if (blackPieces[position].isOccupied()) {
            blackPieces[position]   = EMPTY_SQUARE
            blackPieceCount--
        }
    }

    private fun getColorArray(team: Int): Array<Piece> {
        return  if (team == WHITE) whitePieces else blackPieces
    }



    /**
     * Removes the piece on the specified square.
     *
     * @return true if a piece was removed.
     */
    fun removePiece(squarePosition: Int): Boolean {
        if (squarePosition in 0.until(BOARD_SIZE)){
            pieces[squarePosition] = EMPTY_SQUARE
            removeFromColorArrays(squarePosition)
        }
        return false
    }
    /**
     * Makes a move from [origin] to [endSquare]
     *
     * @return true if a piece was moved
     */

    fun makeMove(move: move) {
        makeMove(move.start(), move.end(), move.getPromotion())
       // println("This move is: ${move.getString()}")
    }

    fun makeMove(origin: Int, endSquare: Int, promotionType: Int = QUEEN): Boolean {
        require(isInBounds(origin) && isInBounds(endSquare)) {"Impossible move: $origin -> $endSquare"}
        require(origin != endSquare) {"Piece cannot null-move: $origin -> $endSquare"}
        val movingPiece = pieces[origin]
        require(movingPiece.isOccupied()) {"There is no piece here: $origin -> $endSquare"}


        handleMoveTypes(movingPiece, origin, endSquare, promotionType)
        lastMove = Pair(origin,endSquare)

                //println("$whitePieceCount,  $blackPieceCount")

        return true
    }

    private fun handleMoveTypes(movingPiece: Piece, origin: Int, endSquare: Int, promotionType: Int = QUEEN) {
        if (isEnpassantMove(movingPiece, origin, endSquare)) {
            removePiece(getSquareBehind(endSquare, movingPiece.color))
        } else if (isCastleMove(movingPiece, origin, endSquare)) {
            makeMove(getAdjacentRookSquare(endSquare), rookDestinationHolder.drop() ?: -1)
        }
        removePiece(origin)
        removePiece(endSquare)

        if (isPromotionMove(movingPiece, endSquare))
            addPiece(movingPiece.promoteTo(promotionType), endSquare) else addPiece(movingPiece.moveThisPiece(), endSquare)


        updateEnpassantSquare(origin, endSquare, movingPiece)
    }

    private fun getAdjacentRookSquare(origin: Int): Int {
        val shortSquare = origin + 1
        val longSquare = origin - 2

        val leftPiece = fetchPiece(shortSquare)
        val rightPiece = fetchPiece(longSquare)


         if (doesNotWrap(origin, shortSquare) && leftPiece.isRook()) {
            rookDestinationHolder.hold(origin - 1)
             return shortSquare
         } else if (doesNotWrap(origin, longSquare) && rightPiece.isRook())  {
            rookDestinationHolder.hold(origin + 1)
            return longSquare
         } else  {
            rookDestinationHolder.drop()
             return -1
         }
    }
    private fun updateEnpassantSquare(origin: Int, endSquare: Int, piece: Piece) {
        enpassantSquare = if (rowDistance(origin, endSquare) == 2 && piece.isPawn()) {
            endSquare
        } else {
            null
        }
    }

    private fun isCastleMove(piece: Piece, origin: Int, endSquare: Int): Boolean {
        return piece.isKing() &&  colDistance(origin, endSquare) == CASTLE_MOVE_DISTANCE
    }
    private fun isEnpassantMove(piece: Piece, origin: Int, endSquare: Int): Boolean {
        return piece.isPawn()
                && enpassantSquare == getSquareBehind(endSquare, piece.color)
                && isOnDiffRow(origin, endSquare)
                && isOnDiffCol(origin, endSquare)
                && fetchPiece(endSquare).isEmpty()


    }
    private fun isPromotionMove(piece: Piece, endSquare: Int): Boolean {
        return piece.isPawn() && isOnBack(endSquare)
    }

    fun fetchEnpassantSquare(): Int? {
        return enpassantSquare
    }

    /**
     * @return an array containing pieces of the specified team (all if left blank)
     */

    fun fetchPieces(team: Int = NO_COLOR): Array<Piece> {
        return if (team == NO_COLOR) return pieces else getColorArray(team)
    }
    fun fetchPiece(squarePosition: Int, color: Int = NO_COLOR): Piece {
        if (isInBounds(squarePosition)) {
            if (color == WHITE) return whitePieces[squarePosition]
            if (color == BLACK) return blackPieces[squarePosition]
            return pieces[squarePosition]
        }
        return EMPTY_SQUARE
    }


    fun getKingPosition(color: Int): Int? {
        return when (color) {
            WHITE ->  whiteKingPosition
            else ->   blackKingPosition
        }
    }
    private fun updateKingPosition(color: Int, square: Int) {
        when (color) {
            WHITE -> whiteKingPosition = square
            else -> blackKingPosition = square
        }
    }

    private fun isInBounds(square: Int): Boolean = square in 0.until(BOARD_SIZE)

    fun getBoardString(orientation: Int = 0): String {
        val board = StringBuilder()
        val line = StringBuilder()
        var rank = 0
        val letters =  "     A   B   C   D   E   F   G   H     "
        val lettersOriented = letters.reversed()
        board.append(lettersOriented + "\n")
        board.append("   +---+---+---+---+---+---+---+---+   \n")

        line.append("|")
        for (square in pieces.indices) {
            val piece = fetchPiece(square)
            line.append(" ${piece.symbol()} |")
            if ((square % 8) == 7 )  {
                if ((square / 8) >= rank) rank++
                line.append(" $rank ")
                board.append(line.toString().reversed() + " $rank " +"\n")
                line.clear()
                board.append("   +---+---+---+---+---+---+---+---+   \n")
                line.append("|")

            }

        }
        board.append(lettersOriented)
        return if (orientation == WHITE) board.reversed().toString() else board.toString()
    }
}