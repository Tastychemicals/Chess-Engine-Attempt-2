package Game
import BoardUtils.*


@JvmInline
value class BitBoard(val board: ULong)



class Board {
    val EMPTY_SQUARE = Piece(pieceCode(0,0))

    private var enpassantSquare: Int?
    private var whiteKingPostion: Int? = null
    private var blackKingPostion: Int? = null


    private val controller: Game
    public val moveGenerator: MoveGenerator
    private var allTypeBitBoards: Array<BitBoard>
    private var pieces: Array<Piece>
     var piecePositions: MutableMap<Int, Piece>
    private var validMoves: HashMap<Int, Set<Int>>

    var lastMove: Pair<Int,Int>
    private val rookDestinationHolder = Holder<Int>()

    constructor(controller: Game) {
        this.moveGenerator = MoveGenerator(this)
        this.controller = controller // each board object created gets a permanent Game.Game object
        this.allTypeBitBoards = initializeBitboards()
        this.pieces = Array<Piece>(BOARD_SIZE) { EMPTY_SQUARE }
        this.piecePositions = pieces.withIndex().associate { (square, piece) -> (square to piece) }.toMutableMap()
        this.lastMove = Pair(-1,-1)
        this.validMoves = HashMap<Int, Set<Int>>()
        enpassantSquare = null
    }

    /**
     * Initializes this board's bit boards.
     */

    fun initializeBitboards(): Array<BitBoard> {
        val whitePawns = BitBoard(0uL)
        val whiteKnights = BitBoard(0uL)
        val whiteBishops = BitBoard(0uL)
        val whiteRooks = BitBoard(0uL)
        val whiteQueens = BitBoard(0uL)
        val whiteKing = BitBoard(0uL)
        val blackPawns = BitBoard(0uL)
        val blackKnights = BitBoard(0uL)
        val blackBishops = BitBoard(0uL)
        val blackRooks = BitBoard(0uL)
        val blackQueens = BitBoard(0uL)
        val blackKing = BitBoard(0uL)
        return arrayOf(
            whitePawns , whiteKnights , whiteBishops , whiteRooks , whiteQueens , whiteKing ,
            blackPawns , blackKnights , blackBishops , blackRooks , blackQueens , blackKing
        )
    }

    fun loadBoard(fenString: String) {
        val digestibleBoard = simplifyFenBoard(fenString)
        for (square in 0.until(BOARD_SIZE)) {
            val piece = getPieceFromFen(digestibleBoard[square])
            pieces[square] = piece
            piecePositions[square] = piece
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
        allTypeBitBoards = initializeBitboards()
        pieces = Array<Piece>(BOARD_SIZE) { EMPTY_SQUARE }
        piecePositions.clear()
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
                pieces[square] = piece
                piecePositions[square] = piece
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
    /**
     * Removes the piece on the specified square.
     *
     * @return true if a piece was removed.
     */
    fun removePiece(squarePosition: Int): Boolean {
        if (squarePosition in 0.until(BOARD_SIZE)){
            pieces[squarePosition] = EMPTY_SQUARE
            piecePositions[squarePosition] = EMPTY_SQUARE
        }
        return false
    }
    /**
     * Makes a move from [origin] to [endSquare]
     *
     * @return true if a piece was moved
     */

    fun makeMove(origin: Int, endSquare: Int): Boolean {
        require(isInBounds(origin) && isInBounds(endSquare)) {"Impossible move: $origin -> $endSquare"}
        val movingPiece = pieces[origin]
        if (!movingPiece.isEmpty() && origin != endSquare) {
            val validMoves = generateMoves(movingPiece.color)
            if (validMoves[origin]?.contains(endSquare) == true ) {


                handleMoveTypes(movingPiece, origin, endSquare)



                lastMove = Pair(origin,endSquare)
                return true
                    }
            }

        return false

    }

    private fun handleMoveTypes(movingPiece: Piece, origin: Int, endSquare: Int) {
        removePiece(origin)
        removePiece(endSquare)

        if (isEnpassantMove(movingPiece, origin, endSquare)) {
            removePiece(getSquareBehind(endSquare, movingPiece.color))
        } else if (isCastleMove(movingPiece, origin, endSquare)) {
            makeMove(getAdjacentRookSquare(endSquare), rookDestinationHolder.drop() ?: -1)
        }
        if (isPromotionMove(movingPiece, endSquare))
            addPiece(movingPiece.promoteTo(QUEEN), endSquare) else addPiece(movingPiece.moveThisPiece(), endSquare)


        updateEnpassantSquare(origin, endSquare, movingPiece)
    }

    private fun getAdjacentRookSquare(origin: Int): Int {
        val shortSquare = origin + 1
        val longSquare = origin - 2

        val leftPiece = fetchPiece(shortSquare)
        val rightPiece = fetchPiece(longSquare)


         if (doesNotWrap(origin, shortSquare) && leftPiece.isRook()) {
            rookDestinationHolder.hold(longSquare)
             return shortSquare
         } else if (doesNotWrap(origin, longSquare) && rightPiece.isRook())  {
            rookDestinationHolder.hold(shortSquare)
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
        if (controller.turn == getPieceColorFromSquare(origin)) {
            if (makeMove(origin,endSquare)) {
                controller.changeTurn()
            }
        }
    }

    fun generateMoves(color: Int): HashMap<Int, MutableSet<Int>> {
        return moveGenerator.genAllLegalMoves(color)
    }

    /**
     * @return an array containing each piece Bit Game.Board.
     */
    fun fetchAllPieces(): Array<Piece> {
        return pieces.copyOf()
    }

    fun fetchPiece(squarePosition: Int): Piece {
        if (isInBounds(squarePosition)) {
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
}