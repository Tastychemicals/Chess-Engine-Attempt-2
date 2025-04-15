package player
import BoardUtils.*
import Game.Board
import Game.Piece
import kotlin.collections.filter
import kotlin.math.abs
import kotlin.math.min

typealias MoveFilter = (Int, Int, Int) -> Boolean
typealias alertTrigger = (Int, Piece, Int) -> Boolean
typealias alertCondition = (Int, Piece) -> Boolean

class MoveGenerator(board: Board) {
    private var referenceBoard = board


    //  Move Information (how should the crawler move?)
    private val pawnMoveInfo = MovementInfo(7, 8, 9, 16, pieceType = PAWN)
    private val knightMoveInfo = MovementInfo(-17, -15, -10,-6, 6, 10, 15, 17, pieceType = KNIGHT, distance = 1)
    private val bishopMoveInfo = MovementInfo(-9, -7, 7, 9, pieceType = BISHOP)
    private val rookMoveInfo = MovementInfo(-8, -1, 1, 8, pieceType = ROOK)
    private val queenMoveInfo = MovementInfo(-9, -8, -7, -1, 1, 7, 8, 9, pieceType = QUEEN)
    private val kingMoveInfo = MovementInfo(-9, -8, -7, -1, 1, 7, 8, 9, pieceType = KING, distance = 1)
    private val noMoveInfo = MovementInfo(pieceType = EMPTY)

    // Move Filters (what moves does the crawler validate?)
    val sliderMoveFilter = { startSquare: Int, newSquare: Int, pastSquare: Int -> isValidSliderMove(startSquare, newSquare, pastSquare) }
    val leaperMoveFilter = { startSquare: Int, newSquare: Int, pastSquare: Int -> isValidLeaperMove(startSquare, newSquare, pastSquare) }
    val pinRayFilter = { startSquare: Int, newSquare: Int, pastSquare: Int ->
        isValidSliderMove(startSquare, newSquare, pastSquare) && (!moveHasDoubleColor(startSquare, newSquare, pastSquare) ) }

    // Special Crawler Flags (what exceptions should the crawler make?)
    val NO_CONDITION = -1
    val XRAY_CONDITION = 1
    val ILLEGAL_CAPTURES = 2
    val CAPTURE_KINGS = 3


    // Crawler Instructions
    val pawnMoveInstructions = MoveInstructions(pawnMoveInfo, leaperMoveFilter)
    val knightMoveInstructions = MoveInstructions( knightMoveInfo, leaperMoveFilter)
    val bishopMoveInstructions = MoveInstructions( bishopMoveInfo, sliderMoveFilter)
    val rookMoveInstructions = MoveInstructions (rookMoveInfo, sliderMoveFilter)
    val queenMoveInstructions = MoveInstructions(queenMoveInfo, sliderMoveFilter)
    val kingMoveInstructions = MoveInstructions( kingMoveInfo, leaperMoveFilter)

    val noInstructions = MoveInstructions(noMoveInfo, leaperMoveFilter)
    val kingXrayInstructions = MoveInstructions(queenMoveInfo, pinRayFilter, XRAY_CONDITION)

    val utilityIntHolder = IntHolder()
    val kingSquareHolder = IntHolder()

    // Types of rays
    val X_RAY = 0
    val IGNORANT_RAY = 1
    val TRAIL_RAY = 0
    val EXACT_RAY = 1



    // Ray conditions (when should a ray remember a move?)
    val pinCondition = {color: Int, piece: Piece  -> piece.isColor(color)}
    val sliderThreatCondition = {color: Int, piece: Piece -> !piece.isColor(color)}


    // Ray Triggers (when should the ray return data?)
    val pinTrigger = { color: Int, piece: Piece, vector: Int -> sliderMatchesVector(piece, vector) && !piece.isColor(color) }
    val sliderThreatTrigger = { color: Int, piece: Piece, vector: Int -> sliderMatchesVector(piece, vector) && !piece.isColor(color) }

    // Ray Instructions (how should the ray cast?)
    val pinRayInstructions = RayInstructions(kingXrayInstructions, pinCondition, pinTrigger, TRAIL_RAY)
    val sliderThreatRayInstructions = RayInstructions(queenMoveInstructions, sliderThreatCondition, sliderThreatTrigger, EXACT_RAY)





    data class MoveInstructions(
        val moveInfo: MovementInfo,
        val moveFilter: MoveFilter,
        val specialCondition: Int = -1,
        private val holder: IntHolder = IntHolder()
    ) {
        fun retrieve(): Int {
            return holder.retrieve()
        }
        fun holder(): IntHolder {
            return holder
        }
        fun valueChanged(): Boolean {
            return holder.hasValueChanged()
        }
    }

    data class RayInstructions(val moveInstructions: MoveInstructions,
                               val alertCondition: alertCondition,
                               val alertTrigger: alertTrigger,
                               val rayType: Int)


    data class MovementInfo(
        val vector1: Int = 0,
        val vector2: Int = 0,
        val vector3: Int = 0,
        val vector4: Int = 0,
        val vector5: Int = 0,
        val vector6: Int = 0,
        val vector7: Int = 0,
        val vector8: Int = 0,
        val distance: Int = BOARD_SIZE,
        val pieceType: Int) {
        private val vectors = listOf(
            this::vector1, this::vector2,
            this::vector3, this::vector4,
            this::vector5, this::vector6,
            this::vector7, this::vector8
        ).map { it -> it.get() }.filter { it != 0 }
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
    private fun getMoveInstructions(type: Int): MoveInstructions {
        val instructions = when(type) {
            PAWN -> pawnMoveInstructions
            KNIGHT -> knightMoveInstructions
            BISHOP -> bishopMoveInstructions
            ROOK -> rookMoveInstructions
            QUEEN -> queenMoveInstructions
            KING -> kingMoveInstructions
            else -> noInstructions
        }
        return instructions
    }


    /**
     * the main outlet called in board
     */
    fun genAllLegalMoves(color: Int): HashMap<Int,MutableSet<Int>> {
        val allMoves = HashMap<Int,MutableSet<Int>>()
        val squaresAndPieces = getAllPieces(color)
        val kingSquare = getKingSquare(color)
        if (kingSquare == null) throw IllegalArgumentException("$color King does not exist")



        val pins = castPinRay(kingSquare, color)
        val kingDefendingSquares = castSliderThreatRay(kingSquare, color)





        // call king validity check here
        for (squareAndPiece in squaresAndPieces) {
            allMoves.put(squareAndPiece.key, genLegalPieceMoves(squareAndPiece, pins, kingDefendingSquares.keys ))
        }
        return allMoves
    }

    fun getCheckedKing(color: Int): Int {
        kingSquareHolder.hold(-1)
        val kingSquare = referenceBoard.getKingPosition(color)
        if (kingSquare != null) {
            val kingDefendingSquares = castSliderThreatRay(kingSquare, getPiece(kingSquare).fetchColor())
            if (kingDefendingSquares.isNotEmpty()) {
                kingSquareHolder.hold(kingSquare)
            }
        }
        return kingSquareHolder.retrieve()

    }


    // for generating actual moves, moves which will be reported in game
    private fun genLegalPieceMoves(squareAndPiece:  Map.Entry<Int, Piece>, pins: Map<Int,Int>, kingDefendingSquares: MutableSet<Int> ): MutableSet<Int> {
        val piece = squareAndPiece.value
        val square = squareAndPiece.key
        return genLegalPieceMoves(square, piece, pins, kingDefendingSquares)
    }
    fun genLegalPieceMoves(square: Int, piece: Piece, pins: Map<Int,Int>, kingDefendingSquares: MutableSet<Int>): MutableSet<Int> {
        var moves = genPseudoPieceMoves(square, piece)

        if (piece.isKing()) {
            kingSquareHolder.hold(-1)
            validateKingMoves(piece.color, moves)

        }else  {
            moves = filterOutPins(square, moves, pins)
            moves = filterDefense(moves, kingDefendingSquares)
        }

        return moves
    }

    private fun validateKingMoves(color: Int, moves: MutableSet<Int>): MutableSet<Int> {
        val kingSquare = getKingSquare(color)
        if (kingSquare != null) {
            moves.removeAll(getOpponentAttackSquares(color))
        }
        //println(moves)
        return moves
    }
    private fun filterOutPins(square: Int, moves: MutableSet<Int>, pins: Map<Int, Int>): MutableSet<Int> {
        if (square in pins.keys) {
            return moves.filter { (vectorBetween(it, square) == abs(pins[square] as Int)) }.toMutableSet()
        }
        return moves
    }
    private fun filterDefense(moves: MutableSet<Int>, defendingSquares: MutableSet<Int>): MutableSet<Int> {
        if (defendingSquares.isNotEmpty()) {
            return moves.filter {  it in defendingSquares }.toMutableSet()
        }
        return moves
    }


    // for generating potential move squares, ignoring check
    private fun genAllPseudoMovesIllegalCaptures(color: Int): HashMap<Int,MutableSet<Int>> {
        val allMoves = HashMap<Int,MutableSet<Int>>()
        val squaresAndPieces = getAllPieces(color)
        for (squareAndPiece in squaresAndPieces) {
            val square = squareAndPiece.key;
            val piece = squareAndPiece.value;
            if (piece.isPawn()) allMoves.put(square, getMoves(square, piece)) else
            allMoves.put(square, crawlMoves(square, color, getMovementInfo(piece.type) ,sliderMoveFilter, ILLEGAL_CAPTURES, utilityIntHolder).toMutableSet())
        }
        return allMoves
    }

    fun genPseudoPieceMoves(square: Int, piece: Piece): MutableSet<Int> {               /// dont detele
        val moves = mutableSetOf<Int>()
        moves.addAll(getMoves(square, piece))
        return moves
    }


    // gets the sum of all pseudo attack squares of the color's opponent (for king safety check)
    fun getOpponentAttackSquares(color: Int): Set<Int> {
        val allPseudoMoves = genAllPseudoMovesIllegalCaptures(getOppositeColor(color))
        var attackSquares = mutableSetOf<Int>()


        for (originSquare in allPseudoMoves.keys) {
            val piece = getPiece(originSquare)
            val squares = allPseudoMoves[originSquare]
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

    // casts a ray as a queen to evaluate pins on king
    fun castPinRay(square: Int, color: Int): Map<Int, Int> {
        return castRay(square, color, pinRayInstructions)
    }
    fun castSliderThreatRay(square: Int, color: Int): MutableMap<Int, Int> {
        return castRay(square,color, sliderThreatRayInstructions)
    }

    fun castThreatRay(square: Int, color: Int) {
        val threatVectors = mutableMapOf<Int,Int>()
        val ray2 = crawlMoves(square,color, knightMoveInstructions).iterator()
    }
    fun castKnightThreatRay() {

    }



    fun castRay(square: Int, color: Int, rayInstructions: RayInstructions): MutableMap<Int, Int> {
        return castRay(
            square, color,
            rayInstructions.moveInstructions, rayInstructions.alertCondition,
            rayInstructions.alertTrigger, rayInstructions.rayType
        )
    }
    fun castRay(
        square: Int, color: Int,
        instructions: MoveInstructions, alertCondition: alertCondition,
        alertTrigger: alertTrigger, rayType: Int): MutableMap<Int, Int>
    {
        val found = mutableMapOf<Int,Int>()
        val alerts = mutableListOf<Int>()
        val ray = crawlMoves(square, color, instructions).iterator()
        while (ray.hasNext()) {
            val nextSquare = ray.next()
            val vector = instructions.retrieve()
            val piece = getPiece(nextSquare)

            // we don't care about old prospects because were now looking in a different direction.
            if (instructions.valueChanged()) {
                alerts.clear()
            }

            if (alertCondition(color, piece)) {     //alert trigger
                alerts.add(nextSquare)
            }

            if (alertTrigger(color, piece, vector)) {
                when (rayType) {
                    TRAIL_RAY -> for (square in alerts) {
                        found.put(square, abs(instructions.retrieve()))
                    }
                    EXACT_RAY ->  {
                        found.putAll( alerts.associateWith { it } )
                        break
                    }
                }
            }
        }
        return found
    }


    fun getAllPieces(color: Int): Map<Int, Piece> {
        return referenceBoard.piecePositions.filter { it.value.color == color && !it.value.isEmpty() }
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


    private fun getMoves(startSquare: Int, piece: Piece): MutableSet<Int> {
        val foundMoves = mutableSetOf<Int>()
        if (piece.isPawn()) foundMoves.addAll(crawlPawnMoves(startSquare, piece))
        else foundMoves.addAll(crawlMoves(startSquare, piece.color, getMoveInstructions(piece.type)))
        return foundMoves
    }

    private fun crawlMoves(
        startSquare: Int,
        color: Int,
        moveInfo: MovementInfo,
        moveFilter:  (Int, Int, Int) -> Boolean,
        specialCondition: Int = -1,
        holder: IntHolder,
    ): Sequence<Int> = sequence {
        for (vector in moveInfo.getVectors()) {
            val tracker = mutableSetOf<Int>()

            for (distance in 1..min(moveInfo.distance, BOARD_WIDTH)) {
                holder.hold(vector) // so that we may check on the direction the iterator is on later
                val newSquare = startSquare + (distance * vector)
                val pastSquare = startSquare + ((distance -1) * vector)
                val targetPiece = getPiece(newSquare)
                // wrapping is never allowed.
                if (doesSliderCrawlerWrap(startSquare, newSquare, pastSquare)) break

                // moves must pass
                if (!(moveFilter(startSquare, newSquare, pastSquare))) break

                when (specialCondition) {
                    XRAY_CONDITION -> if (targetPiece.isColor(color)) {
                        tracker.add(newSquare)

                        if (tracker.size > 1) break
                        yield(newSquare)
                        continue
                    }
                    ILLEGAL_CAPTURES -> if (targetPiece.isColor(color)) {
                        yield(newSquare)
                    } else if (targetPiece.isKing()) continue
                }

                if (targetPiece.isOccupied()) {
                    if (collisionIsCapture(newSquare, color)) {
                        yield(newSquare)
                    }
                    //if (specialCondition != XRAY_CONDITION) {
                        break
                   // }
                }
                yield(newSquare)
            }
        }
    }
    private fun crawlMoves(startSquare: Int, color: Int, moveInstructions: MoveInstructions): Sequence<Int> {
        return crawlMoves(
            startSquare, color,
            moveInstructions.moveInfo, moveInstructions.moveFilter,
            moveInstructions.specialCondition, moveInstructions.holder())
    }

    private fun crawlPawnMoves(startSquare: Int, piece: Piece): MutableSet<Int> {
        val validSquares = mutableSetOf<Int>()
        var canJump = false

        for (vector in pawnMoveInfo.getVectors()) {
            val newSquare = startSquare + (vector * getPawnDirection(piece.color))


            if (isDiagonalVector(vector)
                && (isValidPawnCapture( startSquare, newSquare, piece.color)
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
    private fun doesSliderCrawlerWrap(origin: Int, endSquare: Int, pastSquare: Int): Boolean =
        (isOnEdge(origin) && isOnEdge(endSquare) && doesCrawlerWrap(pastSquare, endSquare)
                || !isOnEdge(origin) && isOnEdge(endSquare) && isOnEdge(pastSquare))

    private fun isValidSliderMove(origin: Int, endSquare: Int, pastSquare: Int): Boolean {
        return isInBounds(endSquare) && !doesSliderCrawlerWrap(origin, endSquare, pastSquare)
    }
    private fun isValidLeaperMove(origin: Int, endSquare: Int, color: Int,): Boolean {
        return isInBounds(endSquare) &&  (!doesCrawlerWrap(origin, endSquare)
                && (collisionIsCapture(endSquare, color) || isEmptySquare(endSquare)))
    }

    private fun moveHasDoubleColor(origin: Int, endSquare: Int, pastSquare: Int): Boolean {
        return (getPiece(pastSquare).isTeamedWith(getPiece(endSquare))) && (pastSquare != origin)
    }


    private fun collisionIsCapture(square: Int, color: Int): Boolean {
        val collisionSquare = getPiece(square)
        if (collisionSquare.isOccupied()) {
            return !collisionSquare.isColor(color)
        }
        return false
    }
    private fun isValidPawnCapture(origin: Int, endSquare: Int, color: Int,): Boolean {
        if (getPiece(origin).isEmpty()) return false
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

    private fun sliderMatchesVector(piece: Piece, vector: Int): Boolean {
        return ((piece.isDiagonalPiece() && isDiagonalVector(vector))
                || (piece.isLinePiece() && isLineVector(vector)))
        //&& piece.isColor(getOppositeColor(piece.color))
    }

    private fun isDiagonalMove(origin: Int, endSquare: Int): Boolean {
        return rowDistance(origin, endSquare) == colDistance(origin, endSquare)
    }
    private fun isLineMove(origin: Int, endSquare: Int): Boolean {
        return (rowDistance(origin, endSquare) == 0 && colDistance(origin, endSquare) != 0) ||
                (rowDistance(origin, endSquare) != 0 && colDistance(origin, endSquare) == 0)
    }

    private fun isEmptySquare(square: Int): Boolean {
        return getPiece(square).isEmpty()
    }
    private fun getPiece(square: Int): Piece {
        return referenceBoard.fetchPiece(square)
    }
    private fun getKingSquare(color: Int): Int? {
        return referenceBoard.getKingPosition(color)
    }
    private fun isInBounds(square: Int): Boolean {
        return !(square > BOARD_SIZE || square < 0)
    }


    // todo: fix board rendering and update structures to use pieces and bit boards
    // todo: implement the BoardUtils.pieceCode encoder
}