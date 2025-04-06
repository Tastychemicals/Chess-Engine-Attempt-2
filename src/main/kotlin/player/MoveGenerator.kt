package player
import BoardUtils.*
import Game.Board
import BoardUtils.BoardHelper.*
import Game.Piece
import kotlin.math.abs

class MoveGenerator(board: Board) {
    private var referenceBoard = board
    private val pawnMoveInfo = MovementInfo(7, 9, 8, 16, pieceType = PAWN)
    private val knightMoveInfo = MovementInfo(17,10, 6, 15, pieceType = KNIGHT)
    private val bishopMoveInfo = MovementInfo(7,9, pieceType = BISHOP)
    private val rookMoveInfo = MovementInfo(1,8, pieceType = ROOK)
    private val queenMoveInfo = MovementInfo(1,8,7,9, pieceType = QUEEN)
    private val kingMoveInfo = MovementInfo(1,8,7,9, pieceType = KING)
    private val noMoveInfo = MovementInfo(pieceType = EMPTY)

    fun generateAllMoves(color: Int): HashMap<Int,Set<Int>> {
        val allMoves = HashMap<Int,Set<Int>>()
        val squaresAndPieces = referenceBoard.piecePositions.filter { it.value.color == color && !it.value.isEmpty() }
        for (squareAndPiece in squaresAndPieces) {
            val piece = squareAndPiece.value
            val square = squareAndPiece.key
            allMoves.put(squareAndPiece.key, generatePieceMoves(square, piece))
        }
        return allMoves
    }

    fun generatePieceMoves(square: Int, piece: Piece): MutableSet<Int> {
        if (piece.isQueen() || piece.isKing()) {
            val moves = crawl(square, Piece(pieceCode(piece.color, ROOK)), piece)
            moves.addAll(crawl(square, Piece(pieceCode(piece.color, BISHOP)), piece))
            return moves
        }
        return crawl(square, piece, piece)
    }


    fun perft() {

    }

    fun orderMoves () {

    }

    fun cloneBoard(board: Board) {

    }

    fun setReferenceBoard(board: Board) {
        referenceBoard = board
    }



    private data class MovementInfo(val vector1: Int = 0,
                                    val vector2: Int = 0,
                                    val vector3: Int = 0,
                                    val vector4: Int = 0,
                                    val pieceType: Int) {

        private val vectors = listOf(this::vector1, this::vector2, this::vector3, this::vector4).map { it -> it.get() }.filter { it != 0}
        fun getVectors(): List<Int> = vectors

    }


    private fun getMovementInfo(pieceType: Int): MovementInfo {
       return when (pieceType) {
           PAWN -> pawnMoveInfo
           KNIGHT -> knightMoveInfo
           BISHOP -> bishopMoveInfo
           ROOK -> rookMoveInfo
           QUEEN -> queenMoveInfo
           KING -> kingMoveInfo
           else -> noMoveInfo
        }
    }

    private fun crawl(startSquare: Int, piece: Piece, truePiece: Piece): MutableSet<Int> {
        val moveInfo = getMovementInfo(piece.type)
        val foundMoves = mutableSetOf<Int>()
        if (piece.isPawn()) {
            var canJump = false
            for(vector in moveInfo.getVectors()) {
                val newSquare = startSquare + (vector * getPawnDirection(piece.color))

                if (isDiagonalVector(vector) && isValidPawnCapture(piece, startSquare, newSquare)) {
                        foundMoves.add(newSquare)
                }

                if (isForwardVector(vector) && isEmptySquare(newSquare)) {
                    if (rowDistance(startSquare, newSquare) == 1) {
                        canJump = true
                        foundMoves.add(newSquare)
                    } else {
                        if (canJump && !piece.hasMoved) {
                            foundMoves.add(newSquare)
                        }
                    }

                }
            }

        } else {
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
                        if (leaperAndWrapsAround(piece, startSquare, newSquare)
                            || sliderAndWrapsAround(piece, startSquare, newSquare, pastSquare)) break

                        //pieces can capture enemies
                        if (collisionIsCapture(newSquare, truePiece.color)) {
                            foundMoves.add(newSquare)
                            break
                        } else {
                            // pieces can move to empty squares
                            if (isEmptySquare(newSquare)) foundMoves.add(newSquare) else break
                        }

                        //kings and knights have a depth of 1
                        if (piece.isLeaper() || truePiece.isLeaper()) break



                    }
                }
            }
        }
        return foundMoves
    }

    private fun doesCrawlerWrap(origin: Int, endSquare: Int): Boolean =

        ((colDistance(origin, endSquare) > 2))
    private fun leaperAndWrapsAround(piece: Piece, origin: Int, endSquare: Int): Boolean =
        piece.isLeaper() && doesCrawlerWrap(origin, endSquare)
    private fun sliderAndWrapsAround(piece: Piece, origin: Int, endSquare: Int, pastSquare: Int): Boolean =
        piece.isSlider() && (isOnEdge(origin) && isOnEdge(endSquare) && doesCrawlerWrap(pastSquare, endSquare)
                || !isOnEdge(origin) && isOnEdge(endSquare) && isOnEdge(pastSquare))
    private fun collisionIsCapture(square: Int, color: Int): Boolean {
        val collisionSquare = referenceBoard.fetchPiece(square)
        if (!collisionSquare.isEmpty()) {
            return collisionSquare.color != color
        }
        return false
    }
    private fun isValidPawnCapture(piece: Piece, origin: Int, endSquare: Int): Boolean {
        return collisionIsCapture(endSquare, piece.color)
                && !doesCrawlerWrap(origin, endSquare)
    }
   // isValidPawn

    private fun getPawnDirection(color: Int): Int = when (color) {
        1 -> 1
        else -> -1
    }

    private fun isOnEdge(square: Int): Boolean = (square % 8 == 0 || square % 8 == 7)
    private fun isDiagonalVector(vector: Int): Boolean {
        return abs(vector) == 7 || abs(vector) == 9
    }
    private fun isForwardVector(vector: Int): Boolean {
        return abs(vector) % BOARD_WIDTH == 0;
    }

    private fun isEmptySquare(square: Int): Boolean {
        return referenceBoard.fetchPiece(square).isEmpty()
    }


    private fun colDistance(origin: Int, endSquare: Int): Int =
        abs(convertIntToPairSquare(origin).first - convertIntToPairSquare(endSquare).first)
    private fun rowDistance(origin: Int, endSquare: Int): Int =
        abs(convertIntToPairSquare(origin).second - convertIntToPairSquare(endSquare).second)

    private fun isInBounds(square: Int): Boolean {
        return !(square > BOARD_SIZE || square < 0)
    }





    // todo: fix board rendering and update structures to use pieces and bit boards
    // todo: implement the BoardUtils.pieceCode encoder
}