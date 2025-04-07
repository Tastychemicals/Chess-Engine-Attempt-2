package player
import BoardUtils.*
import Game.Board
import Game.Piece
import kotlin.collections.filter
import kotlin.math.abs

class MoveGenerator(board: Board) {
    private var referenceBoard = board
    private val pawnMoveInfo = MovementInfo(7, 8, 9, 16, pieceType = PAWN)
    private val knightMoveInfo = MovementInfo(-17, -15, -10,-6, 6, 10, 15, 17, pieceType = KNIGHT)
    private val bishopMoveInfo = MovementInfo(-9, -7, 7, 9, pieceType = BISHOP)
    private val rookMoveInfo = MovementInfo(-8, -1, 1, 8, pieceType = ROOK)
    private val queenMoveInfo = MovementInfo(-9, -8, -7, -1, 1, 7, 8, 9, pieceType = QUEEN)
    private val kingMoveInfo = MovementInfo(-9, -8, -7, -1, 1, 7, 8, 9, pieceType = KING)
    private val noMoveInfo = MovementInfo(pieceType = EMPTY)



    val findXrays = { newPiece: Piece, originColor: Int, vector: Int, newSquare: Int ->
        (!newPiece.isEmpty() && newPiece.color != originColor)

        && ((isDiagonalVector(vector) && getPiece(newSquare).isDiagonalPiece())

        || (isLineVector(vector) && getPiece(newSquare).isLinePiece()))
    }




   // var x = rayScanInfo(referenceBoard::getKingPosition, )

//    private val findXrays = rayScanInfo(scanCondition = !newPiece.isEmpty()
//            && newPiece.color != color
//            && (isDiagonalVector(vector) && getPiece(newSquare).isDiagonalPiece())
//            || (isLineVector(vector) && getPiece(newSquare).isLinePiece())))


    /**
     * the main outlet called in board
     */
    fun genAllLegalMoves(color: Int): HashMap<Int,MutableSet<Int>> {
        val allMoves = HashMap<Int,MutableSet<Int>>()
        val squaresAndPieces = referenceBoard.piecePositions.filter { it.value.color == color && !it.value.isEmpty() }
        // call king validity check here
        for (squareAndPiece in squaresAndPieces) {
            allMoves.put(squareAndPiece.key, genLegalPieceMoves(squareAndPiece))
        }
        return allMoves
    }

    fun genLegalPieceMoves(square: Int, piece: Piece): MutableSet<Int> {
        val moves = genPseudoPieceMoves(square, piece)
        when  {
            piece.isKing() -> validateKingMoves(piece.color, moves)

        }
        return moves
    }
    private fun genLegalPieceMoves(squareAndPiece:  Map.Entry<Int, Piece>): MutableSet<Int> {
        val piece = squareAndPiece.value
        val square = squareAndPiece.key
        return genLegalPieceMoves(square, piece)
    }



    fun genAllPseudoMoves(color: Int, isRestrainingPawns: Boolean = true, ignoreTeam: Boolean = false): HashMap<Int,MutableSet<Int>> {
        val allMoves = HashMap<Int,MutableSet<Int>>()
        val squaresAndPieces = referenceBoard.piecePositions.filter { it.value.color == color && !it.value.isEmpty() }
        for (squareAndPiece in squaresAndPieces) {
            allMoves.put(squareAndPiece.key,  genPseudoPieceMoves(squareAndPiece, isRestrainingPawns, ignoreTeam))
        }
        return allMoves
    }

    //piece.isPawn() -> moves.filter { isValidPawnCapture(getOppositeColor(piece.color), it, square ) }





    fun genPseudoPieceMoves(square: Int, piece: Piece, isRestrainingPawns: Boolean = true, ignoreTeam: Boolean = false): MutableSet<Int> {
        val moves = mutableSetOf<Int>()
        when {
            piece.isSlider() -> moves.addAll(crawlSliderMoves(square, piece, ignoreTeam))
            piece.isLeaper() -> moves.addAll(crawlLeaperMoves(square, piece, ignoreTeam))
            piece.isPawn() ->   moves.addAll(crawlPawnMoves(square, piece, isRestrainingPawns))

        }
        val l = referenceBoard.getKingPosition(piece.color)
        if (l != null) {
            //println(findXrayers(l, piece.color))
            //println(findAttackers(l, piece.color))
        }
        return moves
    }
    private fun genPseudoPieceMoves(set: Map.Entry<Int, Piece>, isRestrainingPawns: Boolean = true, ignoreTeam: Boolean = false): MutableSet<Int> {
        val piece = set.value
        val square = set.key
        return genPseudoPieceMoves(square, piece, isRestrainingPawns, ignoreTeam)
    }


    fun generateAllPseudoMoveSquares(color: Int): Set<Int> {
        val allMoves = mutableSetOf<Int>()
        val squaresAndPieces = referenceBoard.piecePositions.filter { it.value.color == color && !it.value.isEmpty() }

        for (squareAndPiece in squaresAndPieces) {
            allMoves.addAll(genPseudoPieceMoves(squareAndPiece))
        }
        return allMoves
    }

    fun generateAllPseudoAttackSquares(color: Int): Set<Int> {
        val allPseudoMoves = genAllPseudoMoves(color, false, true);
        var attackSquares = mutableSetOf<Int>()


        for (originSquare in allPseudoMoves.keys) {
            val piece = getPiece(originSquare)
            val squares = allPseudoMoves.get(originSquare)
            if (squares != null) {
            if (piece.isPawn()) {
                attackSquares.addAll(squares.filter { isDiagonalMove(it, originSquare) }.toSet())
            } else {
                    attackSquares.addAll(squares)
            }
        }
        }

        return attackSquares
    }


    fun validateKingMoves(color: Int, moves: MutableSet<Int>): MutableSet<Int> {
        val kingSquare = referenceBoard.getKingPosition(color)
        if (kingSquare != null) {
           moves.removeAll(generateAllPseudoAttackSquares(getOppositeColor(color)))
            for (move in moves) {
               // if
            }
        }
        println(moves)
        return moves
    }

    fun castthreatRay() {

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



    protected data class MovementInfo(
        val vector1: Int = 0,
        val vector2: Int = 0,
        val vector3: Int = 0,
        val vector4: Int = 0,
        val vector5: Int = 0,
        val vector6: Int = 0,
        val vector7: Int = 0,
        val vector8: Int = 0,
        val pieceType: Int) {

        private val vectors = listOf(
            this::vector1, this::vector2,
            this::vector3, this::vector4,
            this::vector5, this::vector6,
            this::vector7, this::vector8).map { it -> it.get() }.filter { it != 0}
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



    private fun findXrayers(startSquare: Int, color: Int, moveInfo: MovementInfo = queenMoveInfo): MutableSet<Int> {
        val foundSquares = mutableSetOf<Int>()
        for (vector in moveInfo.getVectors()) {
            for (distance in 1..BOARD_WIDTH) {
                val newSquare = startSquare + (distance *  vector)
                val pastSquare = startSquare + ((distance - 1) * vector)
                val newPiece = getPiece(newSquare)
                if (!(isValidSliderMove(newPiece, startSquare, newSquare, pastSquare))) {
                    break
                }
                if ((!newPiece.isEmpty()
                    && newPiece.color != color)
                    && ((isDiagonalVector(vector) && getPiece(newSquare).isDiagonalPiece())
                    || (isLineVector(vector) && getPiece(newSquare).isLinePiece()))) {

                            foundSquares.add(newSquare)


                    }
                }
            }

        return foundSquares
    }


    private fun findAttackers(startSquare: Int, color: Int,): MutableSet<Int> {
        val foundSquares = mutableSetOf<Int>()
        val moveInfoList: List<MovementInfo> = listOf<MovementInfo>(queenMoveInfo, knightMoveInfo)


        return foundSquares
    }
    private fun getAllEnemyMoveSquares(startSquare: Int, color: Int, moveInfo: MovementInfo = queenMoveInfo): MutableSet<Int> {
        val foundSquares = mutableSetOf<Int>()
        for (vector in moveInfo.getVectors()) {
            for (distance in 1..BOARD_WIDTH) {
                val newSquare = startSquare + (distance *  vector)
                val pastSquare = startSquare + ((distance - 1) * vector)
                val newPiece = getPiece(newSquare)
                if (!(isValidSliderMove(newPiece, startSquare, newSquare, pastSquare))) {
                    break
                }
                if ((!newPiece.isEmpty())) {
                    if (newPiece.color != color
                        && (
                                (isDiagonalVector(vector) && newPiece.isDiagonalPiece())
                                || (isLineVector(vector) && newPiece.isLinePiece())
                                || (isValidPawnCapture(newPiece.color, startSquare, newSquare))))
                    {
                        foundSquares.add(newSquare)
                    } else {
                        break
                    }
                }

            }
        }

        return foundSquares
    }

    private fun crawlSliderMoves(startSquare: Int, piece: Piece, ignoreTeam: Boolean = false): MutableSet<Int> {
        val foundMoves = mutableSetOf<Int>()
        val moveInfo = getMovementInfo(piece.type)
        if (!piece.isSlider()) return foundMoves

        for (vector in moveInfo.getVectors()) {
            for (distance in 1..BOARD_WIDTH) {
                val newSquare = startSquare + (distance *  vector)
                val pastSquare = startSquare + ((distance - 1) * vector)


                if (!(isValidSliderMove(piece, startSquare, newSquare, pastSquare))) {
                    break
                }

                when  {
                    collisionIsCapture(newSquare, piece.color) -> { foundMoves.add(newSquare); break}
                    isEmptySquare(newSquare) -> foundMoves.add(newSquare)
                    ignoreTeam ->  { foundMoves.add(newSquare); break }
                    else -> break
                }
            }
        }
        return foundMoves
    }

    private fun crawlSliderMoves(startSquare: Int, color: Int, pieceType: Int, ignoreTeam: Boolean = false): MutableSet<Int> {
        return crawlSliderMoves(startSquare, Piece(pieceCode(color, pieceType)))
    }

    private fun crawlLeaperMoves(startSquare: Int, piece: Piece, ignoreTeam: Boolean = false): MutableSet<Int> {
        val foundMoves = mutableSetOf<Int>()
        if (!piece.isLeaper()) return foundMoves

        val moveInfo = getMovementInfo(piece.type)

            for (vector in moveInfo.getVectors()) {
                    val newSquare = startSquare + vector
                    if (isValidLeaperMove(piece, startSquare, newSquare) || ignoreTeam) {
                        foundMoves.add(newSquare)
                    }
            }
        return foundMoves
    }

    private fun crawlPawnMoves(startSquare: Int, piece: Piece, isRestrainingPawns: Boolean = true): MutableSet<Int> {
        val validSquares = mutableSetOf<Int>()
        var canJump = false
        if (!piece.isPawn()) return validSquares

        for (vector in pawnMoveInfo.getVectors()) {
            val newSquare = startSquare + (vector * getPawnDirection(piece.color))

            if(!isRestrainingPawns) {
                validSquares.add(newSquare)
                continue
            }
            if (isDiagonalVector(vector)
                && (isValidPawnCapture(piece.color, startSquare, newSquare)
                || isEnpassantCapture(newSquare, piece.color))) {
                validSquares.add(newSquare)
            }

            if (isForwardVector(vector) && isEmptySquare(newSquare)) {
                if (rowDistance(startSquare, newSquare) == 1) {
                    canJump = true
                    validSquares.add(newSquare)
                } else {
                    if (canJump && !piece.hasMoved) {
                        validSquares.add(newSquare)
                    }
                }
            }
        }
        return validSquares
    }



    private fun doesCrawlerWrap(origin: Int, endSquare: Int): Boolean = ((colDistance(origin, endSquare) > 2))
//    private fun leaperAndWrapsAround(piece: Piece, origin: Int, endSquare: Int): Boolean =
//        piece.isLeaper() && doesCrawlerWrap(origin, endSquare)
    private fun doesSliderCrawlerWrap(origin: Int, endSquare: Int, pastSquare: Int): Boolean =
        (isOnEdge(origin) && isOnEdge(endSquare) && doesCrawlerWrap(pastSquare, endSquare)
                || !isOnEdge(origin) && isOnEdge(endSquare) && isOnEdge(pastSquare))

    private fun isValidSliderMove(piece: Piece, origin: Int, endSquare: Int, pastSquare: Int): Boolean {
        return isInBounds(endSquare) && !doesSliderCrawlerWrap(origin, endSquare, pastSquare)
    }
    private fun isValidLeaperMove(piece: Piece, origin: Int, endSquare: Int): Boolean {
        return isInBounds(endSquare) &&  (!doesCrawlerWrap(origin, endSquare)
                && (collisionIsCapture(endSquare, piece.color) || isEmptySquare(endSquare)))
    }

    private fun collisionIsCapture(square: Int, color: Int): Boolean {
        val collisionSquare = getPiece(square)
        if (!collisionSquare.isEmpty()) {
            return collisionSquare.color != color
        }
        return false
    }
    private fun isValidPawnCapture(color: Int, origin: Int, endSquare: Int): Boolean {
        return collisionIsCapture(endSquare, color)
                && !doesCrawlerWrap(origin, endSquare) && isDiagonalMove(origin, endSquare)
    }
    private fun isEnpassantCapture(square: Int, color: Int): Boolean {
        return getSquareBehind(square, color) == referenceBoard.fetchEnpassantSquare()
    }


    private fun isOnEdge(square: Int): Boolean = (square % 8 == 0 || square % 8 == 7)
    private fun isDiagonalVector(vector: Int): Boolean {
        return abs(vector) == 7 || abs(vector) == 9
    }
    private fun isLineVector(vector: Int): Boolean {
        return abs(vector) == 8 || abs(vector) == 1
    }

    private fun isKnightVector(vector: Int): Boolean {
        return abs(vector) == 6 || abs(vector) == 10 || abs(vector) == 15 || abs(vector) == 17
    }
    private fun isForwardVector(vector: Int): Boolean {
        return abs(vector) % BOARD_WIDTH == 0;
    }


    private fun vectorBetween(origin: Int, endSquare: Int): Int {
        val difference = abs(endSquare - origin)
        val vectorBetween = when  {
            rowDistance(origin, endSquare) == 0 -> 1
            difference % 8 == 0 -> 8
            difference % 9 == 0 -> 9
            difference % 7 == 0 -> 7
           else -> difference

        }

    return vectorBetween
    }

    private fun isDiagonalMove(origin: Int, endSquare: Int): Boolean {
        return rowDistance(origin, endSquare) == colDistance(origin, endSquare)
    }

    private fun isLineMove(origin: Int, endSquare: Int): Boolean {
        return (rowDistance(origin, endSquare) == 0 && colDistance(origin, endSquare) != 0) ||
                (rowDistance(origin, endSquare) != 0 && colDistance(origin, endSquare) == 0)
    }

    private fun isEmptySquare(square: Int): Boolean {
        return referenceBoard.fetchPiece(square).isEmpty()
    }
    private fun getPiece(square: Int): Piece {
        return referenceBoard.fetchPiece(square)
    }




    private fun isInBounds(square: Int): Boolean {
        return !(square > BOARD_SIZE || square < 0)
    }





    // todo: fix board rendering and update structures to use pieces and bit boards
    // todo: implement the BoardUtils.pieceCode encoder
}