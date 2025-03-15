import BoardHelper.Companion.convertPairToIntSquare
import BoardHelper.Companion.pieceCode


@JvmInline
value class BitBoard(val board: ULong)

@JvmInline
value class Piece(val bitCode: UInt) {

    // the lsb of each value needs to be in first place value -> 0 000    // 000000
    val color get() = getProperty(BoardHelper.COLOR_SELECTOR, 3)
    val type get() = getProperty(BoardHelper.TYPE_SELECTOR, 0)


    // do pieces get a few extra bits to store their bitboard?

    fun isEmpty(): Boolean {
        return type == BoardHelper.EMPTY
    }
    fun isKing(): Boolean {
        return type == BoardHelper.KING
    }

    private fun getProperty(selector: UInt, shift: Int): Int {
        return ((bitCode and selector) shr shift).toInt()
    }

    override fun toString(): String {
        return("${BoardHelper.colors[color]} ${BoardHelper.typeNames[type]}")
    }
}

class Board {
    val EMPTY_SQUARE = Piece(pieceCode(0,0))

    private val controller: Game
    private var allTypeBitBoards: Array<BitBoard>
    private var pieces: Array<Piece>
    private var piecePositions: MutableMap<Int, Piece>
    var lastMove: Pair<Int,Int>

    constructor(controller: Game) {
        this.controller = controller // each board object created gets a permanent Game object
        this.allTypeBitBoards = initializeBitboards()
        this.pieces = Array<Piece>(BOARD_SIZE) { EMPTY_SQUARE }
        this.piecePositions = pieces.withIndex().associate { (square, piece) -> (square to piece) }.toMutableMap()
        this.lastMove = Pair(-1,-1)



        controller.turn
       // println(piecePositions)
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

    
    // FRAGILE FEN CODE. DO NOT TOUCH//todo: rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
    fun loadBoardq(fenString: String) {
        clearBoard()
        var skippedSquares = 0
        val fenBoard = fenString.split("/")

        for (rank in 0..7) {// loop over the 8 layers
            val fenlayer = fenBoard[rank]
            println("file: ${rank} , layer: ${fenlayer}")
            for (file in 0..7) {

                val fenPiece = if (file < fenlayer.length) {
                    fenlayer[file]
                } else if (file - skippedSquares in 0..fenlayer.length - 1) {
                    fenlayer[file - skippedSquares]
                } else 'e'


                // println("fenpiece: ${fenPiece} , layer: ${fenlayer}, idx < layer length: ${file < fenlayer.length}")  //squares beyond length of this iteration are excluded
                val position = BoardHelper.convertPairToIntSquare(Pair(file, rank))
                val piece = BoardHelper.getPieceFromFen(fenPiece)

                if (fenPiece.isDigit()) {
                    pieces[position] = EMPTY_SQUARE
                    skippedSquares += fenPiece.digitToInt() - 1
                    println("square skipped at: $position, skipped: $skippedSquares")
                } else {
                    pieces[position + skippedSquares] = piece
                    if (addPiece(piece, position + skippedSquares)) {
                        println("piece added on square $position")
                    }
                    println("piece not added to square $position")
                    skippedSquares = 0
                }
            }
            skippedSquares = 0
        }
    }

    fun loadBoard(fenString: String) {
        val digestibleBoard = BoardHelper.simplifyFenBoard(fenString)
        for (square in 0.until(BOARD_SIZE)) {
            pieces[square] = BoardHelper.getPieceFromFen(digestibleBoard[square])
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
       // piecePositions = pieces.withIndex().associate { (square, piece) -> (square to piece) }.toMutableMap()
        return true
    }
    /**
     * Places a Piece on the specified bitboard if the
     * specified position is empty on the board.
     *
     * @return true if a piece was added.
     */
    fun addPiece(piece: Piece, square: Int): Boolean {
        if (square in 0.until(BOARD_SIZE) && !piece.isEmpty()) {
            if (pieces[square].isEmpty()) {
                pieces[square] = piece
                piecePositions[square] = piece
                return true
            }
        }
        return false
    }

    fun addPiece(color: Int, type: Int, squarePosition: Int): Boolean {
        if (squarePosition in 0.until(BOARD_SIZE) && type != BoardHelper.EMPTY){
            return addPiece(Piece(pieceCode(color, type)), squarePosition)
        }
        allTypeBitBoards[type].board shl
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
        if (origin in 0.until(BOARD_SIZE) && endSquare in 0.until(BOARD_SIZE)) {
            val movingPiece = pieces[origin]
            if (!movingPiece.isEmpty() && origin != endSquare){
                removePiece(origin)
                removePiece(endSquare)
                addPiece(movingPiece, endSquare)
                lastMove = Pair(origin,endSquare)
            }


            return true
        }
        return false

    }

    fun tryMove(origin: Int, endSquare: Int) {
        if (controller.turn == getPieceColorFromSquare(origin)) {
            makeMove(origin,endSquare)
            controller.changeTurn()
        }
    }

    /**
     * @return an array containing each piece Bit Board.
     */
    fun fetchAllPieces(): Array<Piece> {
        return pieces
    }

    fun fetchPiece(squarePosition: Int): Piece {
        if (squarePosition != -1 && squarePosition in 0.rangeTo(BOARD_SIZE)) {
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





}