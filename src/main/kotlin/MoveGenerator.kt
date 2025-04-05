import kotlin.math.abs

typealias moveConstraint = (Int, Int) -> Boolean

class MoveGenerator(board: Board) {
    val referenceBoard = board

    //val pawnMoveInfo = MovementInfo()
    val knightMoveInfo = MovementInfo(17,10, 6, 15, pieceType = KNIGHT)
    val bishopMoveInfo = MovementInfo(7,9, pieceType = BISHOP)
    val rookMoveInfo = MovementInfo(1,8, pieceType = ROOK)
    val queenMoveInfo = MovementInfo(1,8,7,9, pieceType = QUEEN)
    val kingMoveInfo = MovementInfo(1,8,7,9, pieceType = KING)



    fun generatePieceMoves(squareAndPiece: Map.Entry<Int, Piece>): MutableSet<Int> { // Piece? ...color..position..type
        val piece = squareAndPiece.value
        val square = squareAndPiece.key
        return generatePieceMoves(square, piece)
    }
    data class MovementInfo(val vector1: Int = 0, val vector2: Int = 0, val vector3: Int = 0, val vector4: Int = 0, val pieceType: Int) {

        private val vectors = listOf(this::vector1, this::vector2, this::vector3, this::vector4).map { it -> it.get() }.filter { it != 0}
        fun getVectors(): List<Int> = vectors

    }


    fun generatePieceMoves(square: Int, piece: Piece): MutableSet<Int> {
        if(piece.isQueen() || piece.isKing()) {
           val moves = crawl(square, Piece(BoardHelper.pieceCode(piece.color, ROOK)), piece)
            moves.addAll(crawl(square, Piece(BoardHelper.pieceCode(piece.color, BISHOP)), piece))
            return moves
        }
        return crawl(square, piece, piece)
    }

    fun getMovementInfo(pieceType: Int): MovementInfo {
       return when (pieceType) {
           KNIGHT -> knightMoveInfo
           BISHOP -> bishopMoveInfo
           ROOK -> rookMoveInfo
           QUEEN -> queenMoveInfo
           KING -> kingMoveInfo
           else -> rookMoveInfo
        }
    }

    fun crawl(startSquare: Int, piece: Piece, truePiece: Piece): MutableSet<Int> {
        val moveInfo = getMovementInfo(piece.type)
        val foundMoves = mutableSetOf<Int>()
        for (d in 0..1) {
            for (vector in moveInfo.getVectors()) {
                val direction = when (d) {
                    0 -> 1 * vector
                    1 -> -1 * vector
                    else -> 0
                }
                for (distance in 1..BOARD_WIDTH) {
                    val newSquare = startSquare + (distance * direction)
                    val pastSquare = startSquare + ((distance - 1) * direction)
                    //pieces cannot wrap around the board

                    //check if the piece itself is on the edge, in this case, if the increment is also on the edge
                    // we dont add it

                    //if the piece is not on the edge, then compare the new square to the last iterated square
                    // if they are both on the edge, we do not add the new square
                    if (leaperAndWrapsAround(piece, startSquare, newSquare)
                        || sliderAndWrapsAround(piece, startSquare, newSquare, pastSquare)) {
                        break
                    }

                    // pieces can move to empty squares

                    //pieces can capture enemies
                    if (collisionIsCapture(newSquare, truePiece.color)) {
                        foundMoves.add(newSquare)
                        break
                    } else {
                        if (referenceBoard.fetchPiece(newSquare).isEmpty()) foundMoves.add(newSquare) else break
                    }
                    //kings and knights have a depth of 1
                    if (piece.isLeaper() || truePiece.isLeaper()) {
                        break
                    }


                }
            }



        }
        return foundMoves
    }

    fun doesCrawlerWrap(origin: Int, endSquare: Int): Boolean {
        return ((colDistance(origin, endSquare) > 2)) // && (rowDistance(origin, endSquare) > 0))
               // || (colDistance(origin, endSquare) > 2) && isOnEdge(endSquare)
    }




    fun collisionIsCapture(square: Int, color: Int): Boolean {
        val collisionSquare = referenceBoard.fetchPiece(square)
        if (!collisionSquare.isEmpty()) {
            return collisionSquare.color != color
        }
        return false

    }

    fun isOnEdge(square: Int): Boolean = (square % 8 == 0 || square % 8 == 7)
    fun isOnDiffCol(origin: Int, endSquare: Int): Boolean = BoardHelper.convertIntToPairSquare(origin).first != BoardHelper.convertIntToPairSquare(endSquare).first
    fun isOnDiffRow(origin: Int, endSquare: Int): Boolean = BoardHelper.convertIntToPairSquare(origin).second != BoardHelper.convertIntToPairSquare(endSquare).second
    fun colDistance(origin: Int, endSquare: Int): Int = abs(BoardHelper.convertIntToPairSquare(origin).first - BoardHelper.convertIntToPairSquare(endSquare).first)
    fun rowDistance(origin: Int, endSquare: Int): Int = abs(BoardHelper.convertIntToPairSquare(origin).second - BoardHelper.convertIntToPairSquare(endSquare).second)
    fun leaperAndWrapsAround(piece: Piece, origin: Int, endSquare: Int): Boolean = piece.isLeaper() && doesCrawlerWrap(origin, endSquare)
    fun sliderAndWrapsAround(piece: Piece, origin: Int, endSquare: Int, pastSquare: Int): Boolean = isOnEdge(origin) && isOnEdge(endSquare) && doesCrawlerWrap(pastSquare, endSquare)
    || !isOnEdge(origin) && isOnEdge(endSquare) && isOnEdge(pastSquare)


    fun isInBounds(square: Int): Boolean {
        return !(square % 8 == 0 || square % 8 == 7 || square > BOARD_SIZE || square < 0)
    }

    fun generateAllMoves(color: Int): HashMap<Int,Set<Int>> {
        val allMoves = HashMap<Int,Set<Int>>()
        val squaresAndPieces = referenceBoard.piecePositions.filter { it.value.color == BoardHelper.colors[color] && !it.value.isEmpty() }
        for (squareAndPiece in squaresAndPieces) {
            allMoves.put(squareAndPiece.key, generatePieceMoves(squareAndPiece))
        }
        return allMoves
    }

    fun perft() {

    }

    fun orderMoves () {

    }

    fun cloneBoard(board: Board) {

    }

    // todo: fix board rendering and update structures to use pieces and bit boards
    // todo: implement the pieceCode encoder
}