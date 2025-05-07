package Base
import BoardUtils.*

class Board {
    val EMPTY_SQUARE = Piece(pieceCode(0,0))

    private var enpassantSquare: Int? = null
    private var whiteKingPostion: Int? = null
    private var blackKingPostion: Int? = null


    public val moveGenerator = MoveGenerator(this)

    private var pieces = Array<Piece>(BOARD_SIZE) { EMPTY_SQUARE }
    private var WhitePieces = Array<Piece>(BOARD_SIZE) { EMPTY_SQUARE }
    private var BlackPieces = Array<Piece>(BOARD_SIZE) { EMPTY_SQUARE }
    private var whitePieceCount = 0;
    private var blackPieceCount = 0;


    var lastMove: Pair<Int,Int>? = null
    private val rookDestinationHolder = Holder<Int>()



    /**
     * Initializes this board's bit boards.
     */


    fun loadBoard(fenString: String) {
        val digestibleBoard = simplifyFenBoard(fenString)
        for (square in 0.until(BOARD_SIZE)) {
            val piece = getPieceFromFen(digestibleBoard[square])
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
        WhitePieces = Array<Piece>(BOARD_SIZE) { EMPTY_SQUARE }
        BlackPieces = Array<Piece>(BOARD_SIZE) { EMPTY_SQUARE }

       // piecePositions = pieces.withIndex().associate { (square, piece) -> (square to piece) }.toMutableMap()
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

    private fun addToColorArray(piece: Piece, position: Int) {
        if (piece.isColor(WHITE)) {
            WhitePieces[position] = piece
            whitePieceCount++
        }
        else {
            if (piece.isColor(BLACK)){
                BlackPieces[position] = piece
                blackPieceCount++
            }

        }

    }

    private fun removeFromColorArrays(position: Int) {
        if (WhitePieces[position].isOccupied()) {
            WhitePieces[position] = EMPTY_SQUARE
            whitePieceCount--
        }
        if (BlackPieces[position].isOccupied()) {
            BlackPieces[position]   = EMPTY_SQUARE
            blackPieceCount--
        }
    }

    private fun getColorArray(team: Int): Array<Piece> {
        return  if (team == WHITE) WhitePieces else BlackPieces
    }


    fun addPiece(color: Int, type: Int, squarePosition: Int): Boolean {
        if (isInBounds(squarePosition) && type != EMPTY) {
            return addPiece(Piece(pieceCode(color, type)), squarePosition)
        }
        return false
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
        println("This move is: ${move.getString()}")
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
        removePiece(origin)
        removePiece(endSquare)

        if (isEnpassantMove(movingPiece, origin, endSquare)) {
            removePiece(getSquareBehind(endSquare, movingPiece.color))
        } else if (isCastleMove(movingPiece, origin, endSquare)) {
            makeMove(getAdjacentRookSquare(endSquare), rookDestinationHolder.drop() ?: -1)
        }
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
                && enpassantSquare != null
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

    fun tryMove(origin: Int, endSquare: Int) {

        //moveGenerator.benchmarkMovegen()
        makeMove(origin,endSquare)

    } // move out of class

    /**
     * @return an array containing pieces of the specified team (all if left blank)
     */

    fun fetchPieces(team: Int = NO_COLOR): Array<Piece> {
        return if (team == NO_COLOR) return pieces else getColorArray(team)
    }
    fun fetchPiece(squarePosition: Int, color: Int = NO_COLOR): Piece {
        if (isInBounds(squarePosition)) {
            if (color == WHITE) return WhitePieces[squarePosition]
            if (color == BLACK) return BlackPieces[squarePosition]
            return pieces[squarePosition]
        }
        return EMPTY_SQUARE
    }

    fun getPieceColorFromSquare(square: Int): Int {
        if (square in 0.until(BOARD_SIZE)){
            return pieces[square].color
        }

        return -1
    }

    fun getKingPosition(color: Int): Int? {
        return when (color) {
            WHITE ->  whiteKingPostion
            else ->   blackKingPostion
        }
    }
    private fun updateKingPosition(color: Int, square: Int) {
        when (color) {
            WHITE -> whiteKingPostion = square
            else -> blackKingPostion = square
        }
    }

    fun isInBounds(square: Int): Boolean = square in 0.until(BOARD_SIZE)

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