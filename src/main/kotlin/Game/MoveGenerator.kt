package Game
import BoardUtils.*
import Game.MoveGenerator.MoveInstructions
import kotlin.collections.filter
import kotlin.collections.mutableListOf
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign
import kotlin.system.measureNanoTime

typealias MoveFilter = (Int, Int, Int) -> Boolean
typealias alertTrigger = (Int, Int, Piece, Int, MutableList<Int>) -> Boolean
typealias alertCondition = (Int, Int, Int, Piece) -> Boolean
typealias mappingInfo = (Int, Int, Int) -> Int

class MoveGenerator(board: Board) {
    //------------------------------------------------------------------------------------------------------------------
    /**
                                    Move Information (how should the crawler move?)
    */

    private val pawnMoveInfo = MovementInfo(7, 8, 9, 16, pieceType = PAWN)
    private val knightMoveInfo = MovementInfo(-17, -15, -10,-6, 6, 10, 15, 17, pieceType = KNIGHT, distance = 1)
    private val bishopMoveInfo = MovementInfo(-9, -7, 7, 9, pieceType = BISHOP)
    private val rookMoveInfo = MovementInfo(-8, -1, 1, 8, pieceType = ROOK)
    private val queenMoveInfo = MovementInfo(-9, -8, -7, -1, 1, 7, 8, 9, pieceType = QUEEN)
    private val kingMoveInfo = MovementInfo(-9, -8, -7, -1, 1, 7, 8, 9, pieceType = KING, distance = 1)


    private val noMoveInfo = MovementInfo(pieceType = EMPTY)
    private val pawnAttackInfo = MovementInfo(-9,-7,7,9, pieceType = BISHOP, distance = 1)
    private val castlingMoveInfo = MovementInfo(-1,1, pieceType = ROOK, distance = 4)
    //------------------------------------------------------------------------------------------------------------------

    /**
                                 Move Filters (what moves does the crawler validate?)
     */
    val sliderMoveFilter = { startSquare: Int, newSquare: Int, pastSquare: Int -> isValidSliderMove(startSquare, newSquare, pastSquare) }
    val leaperMoveFilter = { startSquare: Int, newSquare: Int, pastSquare: Int -> isValidLeaperMove(startSquare, newSquare, pastSquare) }

    val pawnAttackFilter = { startSquare: Int, newSquare: Int, pastSquare: Int ->  isValidPawnCapture(startSquare, newSquare, getPiece(startSquare).fetchColor()) }
    val pinRayFilter = { startSquare: Int, newSquare: Int, pastSquare: Int ->
        isValidSliderMove(startSquare, newSquare, pastSquare) && (!moveHasDoubleColor(startSquare, newSquare, pastSquare) ) }
    //------------------------------------------------------------------------------------------------------------------


    /**
                                Crawler Flags (what exceptions should the crawler make?)
     */
    val NO_CONDITION = -1
    val XRAY_CONDITION = 1
    val ILLEGAL_CAPTURES = 2
    //------------------------------------------------------------------------------------------------------------------

    /**
                                                    Crawler Instructions
     */
    val pawnMoveInstructions = MoveInstructions(pawnMoveInfo, leaperMoveFilter)
    val knightMoveInstructions = MoveInstructions( knightMoveInfo, leaperMoveFilter)
    val bishopMoveInstructions = MoveInstructions( bishopMoveInfo, sliderMoveFilter)
    val rookMoveInstructions = MoveInstructions (rookMoveInfo, sliderMoveFilter)
    val queenMoveInstructions = MoveInstructions(queenMoveInfo, sliderMoveFilter)
    val kingMoveInstructions = MoveInstructions( kingMoveInfo, leaperMoveFilter)

    val noInstructions = MoveInstructions(noMoveInfo, leaperMoveFilter)
    val kingXrayInstructions = MoveInstructions(queenMoveInfo, pinRayFilter, XRAY_CONDITION)
    val pawnAttackCheckInstructions =  MoveInstructions(pawnAttackInfo, pawnAttackFilter)
    val castlingInstructions = MoveInstructions(castlingMoveInfo, sliderMoveFilter, ILLEGAL_CAPTURES)

    //------------------------------------------------------------------------------------------------------------------
    /**
     *                                              Holders
     */
    val utilityIntHolder = Holder<Int>()
    val kingSquareHolder = Holder<Int>()
    //------------------------------------------------------------------------------------------------------------------

    /**
                                                 Types of rays
     */


    val NORMAL_RAY = 0     // returns squares, mapped to themselves (returns after trigger is found once)
    val MAPPING_RAY = 1    // returns squares, mapped to whatever the map function is
    val CASTLE_RAY = 2     // returns the list of squares if they satisfy castling requirements. (not very useful)
    val CUMULATIVE_RAY = 3 // same as normal, but triggers after

    //------------------------------------------------------------------------------------------------------------------
    /**
                                    Ray conditions (when should a ray remember a square?)
     */

    val NO_PARAM = -1
    val KING_CASTLE_DISTANCE = 2

    val pinCondition: alertCondition = {color: Int, _, _, piece: Piece  -> piece.isColor(color)}
    val sliderThreatCondition: alertCondition = {color: Int, _, _, piece: Piece -> !piece.isColor(color)}
    val pieceIsEnemyCondition: alertCondition = {color: Int, _, _, piece: Piece -> piece.isOccupied() && !piece.isColor(color)}
    val castlingCondition: alertCondition = {color: Int, square: Int, distance, piece: Piece ->

        (piece.isColor(color)   &&     // must be team
            piece.isRook() && piece.hasMoved) ||
                                                        // add you because the size of the alerts will be too large
                (!piece.isColor(color) && piece.isEmpty() && distance <= KING_CASTLE_DISTANCE) ||

                        piece.isOccupied() && !piece.isRook()

    }



    val pieceIsPawnCondition: alertCondition = {color: Int, _, _, piece: Piece -> pieceIsEnemyCondition(color, NO_PARAM, NO_PARAM, piece) && piece.isPawn()}
    val pieceIsKnightCondition: alertCondition = {color: Int, _, _, piece: Piece ->  pieceIsEnemyCondition(color, NO_PARAM, NO_PARAM, piece) && piece.isKnight()}
    //------------------------------------------------------------------------------------------------------------------

    /**
                                    Ray Triggers (when should the ray return data?)
     */
    val pinTrigger: alertTrigger = { color: Int, square: Int, piece: Piece, vector: Int, alerts: MutableList<Int> -> sliderMatchesVector(piece, vector) && !piece.isColor(color) }
    val sliderThreatTrigger: alertTrigger = { color: Int, square: Int, piece: Piece, vector: Int, alerts: MutableList<Int> -> sliderMatchesVector(piece, vector) && !piece.isColor(color) }
    val alertFoundTrigger: alertTrigger = { color: Int, square: Int, piece: Piece, vector: Int, alerts: MutableList<Int>  -> alerts.isNotEmpty() }
    val edgeTouchedTrigger: alertTrigger = { _, square: Int, piece: Piece, vector: Int, _  -> isOnSide(square) }
    val blockageTrigger: alertTrigger = { color: Int, square: Int, piece: Piece, vector: Int, alerts: MutableList<Int> -> piece.isOccupied()}
    val collisionFoundTrigger: alertTrigger = {color: Int, square: Int, piece: Piece, vector: Int, alerts: MutableList<Int> ->
        piece.isOccupied()
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     *                                  Ray trail Info (What should the trail add?)
     */
    val vectorTrailInfo: mappingInfo = { currentSquare: Int, distance: Int, vector: Int  -> vector}
//    val castlingTrailInfo: mappingInfo = { currentSquare: Int, distance: Int, instructions: MoveInstructions  ->
//        if (getPiece(currentSquare).isRook()) {
//            if (getPiece(currentSquare).hasMoved) {
//                currentSquare
//            }
//            -1
//        } else {
//            if ((currentSquare !in enemyAttackSquares) && distance < 3) {
//                currentSquare
//            }
//            -1
//        }
//    }
//    val noTrailInfo: mappingInfo = { currentSquare: Int, distance: Int, instructions: MoveInstructions  -> -1}
    //------------------------------------------------------------------------------------------------------------------
    /**
                                    Ray Instructions (how should the ray cast?)
     */
    val pinRayInstructions = RayInstructions(kingXrayInstructions, pinCondition, pinTrigger, MAPPING_RAY, vectorTrailInfo)
    val sliderThreatRayInstructions = RayInstructions(queenMoveInstructions, sliderThreatCondition, sliderThreatTrigger, NORMAL_RAY)
    val knightThreatRayInstructions = RayInstructions(knightMoveInstructions, pieceIsKnightCondition, alertFoundTrigger, NORMAL_RAY)
    val pawnThreatRayInstructions = RayInstructions(pawnAttackCheckInstructions, pieceIsPawnCondition, alertFoundTrigger, NORMAL_RAY)
    val castlingRayInstructions = RayInstructions(castlingInstructions, castlingCondition, collisionFoundTrigger, CASTLE_RAY)
    //------------------------------------------------------------------------------------------------------------------


    private var referenceBoard = board
     var enemyAttackSquares = IntArray(64)
    private var kingDefendingSquares = mutableMapOf<Int,Int>()
    private var pins = mutableMapOf<Int,Int>()
    private var attackedKing = -1
    private var kingSquare: Int? = null


    var raysCasted = 0



    data class MoveInstructions(
        val moveInfo: MovementInfo,
        val moveFilter: MoveFilter,
        val specialCondition: Int = -1,
        private val holder: Holder<Int> = Holder()
    ) {
        fun retrieve(): Int {
            return holder.show() ?: 0
        }
        fun holder(): Holder<Int> {
            return holder
        }
        fun valueChanged(): Boolean {
            return holder.hasValueChanged()
        }
    }

    data class RayInstructions(
        val moveInstructions: MoveInstructions,
        val alertCondition: alertCondition,
        val alertTrigger: alertTrigger,
        val rayType: Int,
        val trailInfo: mappingInfo = { currentSquare: Int, distance: Int, vector: Int  -> -1}
    )
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
    private fun getMoveFilter(pieceType: Int): (Int, Int, Int) -> Boolean {
        return when (pieceType) {
            KNIGHT -> leaperMoveFilter
            KING -> leaperMoveFilter
            else -> sliderMoveFilter
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
        var totalMoves = 0
        //val start = measureNanoTime {

        val squaresAndPieces = getAllPieces(color)
        kingSquare = getKingSquare(color)
        require (kingSquare != null) {"$color King does not exist"}
        raysCasted = 0
        pins = castPinRay(kingSquare!!, color)
        enemyAttackSquares = getOpponentAttackSquares(color)
           // println(enemyAttackSquares)
        kingDefendingSquares = castThreatRay(kingSquare!!, color)
        attackedKing = getCheckedKing(color)


        for (squareAndPiece in squaresAndPieces) {
            val moves = genLegalPieceMoves(squareAndPiece)
            allMoves.put(squareAndPiece.key, moves)
            totalMoves += moves.size
        }

      //  }
       // println("rays casted on iteration: $raysCasted with a time of: ${start/1_000.0}micros")
        //println("Total moves found: $totalMoves")
        return allMoves
    }


    fun benchmarkMovegen(durationMillis: Long = 1000) {
        val startTime = System.nanoTime()
        val endTime = startTime + durationMillis * 1_000_000
        var nodes = 0L

        while (System.nanoTime() < endTime) {
            genAllLegalMoves(BLACK)
            nodes++
        }

        val actualDuration = (System.nanoTime() - startTime) / 1_000_000.0
        val nps = nodes / (actualDuration / 1000.0)

        println("Nodes: $nodes in ${"%.2f".format(actualDuration)} ms")
        println("NPS: ${"%.2f".format(nps)} nodes/sec")
    }

    // for generating actual moves, moves which will be reported in game
    private fun genLegalPieceMoves(squareAndPiece:  Map.Entry<Int, Piece>?): MutableSet<Int> {
        require(squareAndPiece != null) {}
            val piece = squareAndPiece.value
            val square = squareAndPiece.key
            return genLegalPieceMoves(square, piece)
    }
    fun genLegalPieceMoves(square: Int, piece: Piece): MutableSet<Int> {
        var moves = genPseudoPieceMoves(square, piece)

        if (piece.isKing()) {

            validateKingMoves(moves)
            if (!piece.hasMoved && attackedKing != square) addCastleMoves(piece.color, square, moves)


        }else  {
            filterOutPins(square, moves, pins)
            filterKingDefense(moves)
        }

        return moves
    }

    private fun validateKingMoves(moves: MutableSet<Int>): MutableSet<Int> {
        moves.removeAll {enemyAttackSquares[it] > 0}
        return moves
    }
    private fun addCastleMoves(color: Int,square: Int, moves: MutableSet<Int>): MutableSet<Int> {
        moves.addAll(castCastleRay(square, color).keys)
        return moves
    }


    private fun filterOutPins(square: Int, moves: MutableSet<Int>, pins: Map<Int, Int>) {
        if (square in pins.keys) { moves.retainAll { abs(vectorBetween(it, square)) == abs(pins[square] as Int) } }
    }
    private fun filterKingDefense(moves: MutableSet<Int>) {
        if (enemyAttackSquares[kingSquare as Int] > 1) { moves.clear(); return }
        if (enemyAttackSquares[kingSquare as Int] > 0) filterDefense(moves, kingDefendingSquares.keys)
    }
    private fun filterDefense(moves: MutableSet<Int>, defendingSquares: MutableSet<Int>) {
        if (defendingSquares.isNotEmpty()) {  moves.retainAll(defendingSquares) }
    }




    // for generating potential move squares, ignoring check
    private fun genAllPseudoMovesIllegalCaptures(color: Int): HashMap<Int,MutableSet<Int>> {
        val allMoves = HashMap<Int,MutableSet<Int>>()
        val squaresAndPieces = getAllPieces(color)
        for (squareAndPiece in squaresAndPieces) {
            val square = squareAndPiece.key;
            val piece = squareAndPiece.value;
            if (piece.isPawn()) allMoves.put(square, crawlPawnMoves(square, piece, ILLEGAL_CAPTURES).toMutableSet()) else
            allMoves.put(square, crawlMoves(square, color, getMovementInfo(piece.type) ,getMoveFilter(piece.type), ILLEGAL_CAPTURES, utilityIntHolder).toMutableSet())
        }
        return allMoves
    }

    fun genPseudoPieceMoves(square: Int, piece: Piece): MutableSet<Int> {               /// dont detele
        val moves = mutableSetOf<Int>()
        moves.addAll(getMoves(square, piece))
        return moves
    }


    // gets the sum of all pseudo attack squares of the color's opponent (for king safety check)
//    fun getOpponentAttackSquares(color: Int): MutableMap<Int, Int> {
//        val allPseudoMoves = genAllPseudoMovesIllegalCaptures(getOppositeColor(color))
//        var attackSquares = mutableMapOf<Int, Int>()
//
//
//        for (originSquare in allPseudoMoves.keys) {
//            val piece = getPiece(originSquare)
//            val squares = allPseudoMoves[originSquare]
//            if (squares != null) {
//                if (piece.isPawn()) {
//                    attackSquares.putAll(squares.filter { isDiagonalMove(it, originSquare) }.associateWith { attackSquares.getOrDefault(it, 0) + 1 })
//                } else {
//                    attackSquares.putAll(squares.associateWith { attackSquares.getOrDefault(it, 0) + 1 })
//
//                }
//            }
//
//        }
//       // println(attackSquares)
//        return attackSquares
//    }

    fun getOpponentAttackSquares(color: Int): IntArray {
        val attackMap = IntArray(64)  // way faster than mutableMap

        for ((square, piece) in getAllPieces(getOppositeColor(color))) {
            val targets = if (piece.isPawn()) {
                crawlPawnMoves(square, piece, ILLEGAL_CAPTURES).filter {
                    isDiagonalMove(it, square)
                }
            } else {
                crawlMovesFast(
                    square,
                    piece.color,
                    getMovementInfo(piece.type),
                    getMoveFilter(piece.type),
                    ILLEGAL_CAPTURES,
                )
            }

            for (target in targets) {
                if (target in 0..63) attackMap[target] += 1
            }
        }

        return attackMap
    }

    fun castRay(square: Int, color: Int, rayInstructions: RayInstructions): MutableMap<Int, Int> {
        return castRay(
            square, color,
            rayInstructions.moveInstructions, rayInstructions.alertCondition,
            rayInstructions.alertTrigger, rayInstructions.rayType, rayInstructions.trailInfo
        )
    }
    fun castRay(
        startSquare: Int, color: Int,
        instructions: MoveInstructions, alertCondition: alertCondition,
        alertTrigger: alertTrigger, rayType: Int, mappingInfo: mappingInfo): MutableMap<Int, Int>
    {
        val found = mutableMapOf<Int,Int>()
        val alerts = mutableListOf<Int>()
        val ray = crawlMoves(startSquare, color, instructions).iterator()
        var distance = 0
        var lastVector = 0
        raysCasted++
        while (ray.hasNext()) {
            val nextSquare = ray.next()
            val vector = vectorBetween(startSquare, nextSquare)  // signed
            val piece = getPiece(nextSquare)
            distance += 1

            // we don't care about old prospects because were now looking in a different direction.
            if (lastVector != vector) {
                alerts.clear()
                distance = 1
                lastVector = vector
            }

            if (alertCondition(color, startSquare, distance, piece)) {     //alert trigger
                alerts.add(nextSquare)
            }

            if (alertTrigger(color, startSquare, piece, vector, alerts)) {
                when (rayType) {
                    MAPPING_RAY -> for (square in alerts) {
                        found.put(square, mappingInfo(square, distance, vector))
                    }
                    NORMAL_RAY ->  {
                        found.putAll( format(alerts) )
                        found.put(startSquare, startSquare)
                        break
                    }

                    CASTLE_RAY -> {
                        if (alerts.size != distance && alerts.size == 2 && alerts.all {  enemyAttackSquares[it] == 0}) {
                            found.putAll(format(alerts))
                        }

                    }
                }
            }
        }
        return found
    }

    fun format(list: MutableList<Int>): Map<Int, Int> {
        return list.associateWith { it }
    }

    // casts a ray as a queen to evaluate pins on king
    fun castPinRay(square: Int, color: Int): MutableMap<Int, Int> {
        return castRay(square, color, pinRayInstructions)
    }
    fun castThreatRay(square: Int, color: Int): MutableMap<Int, Int> {
        val threats = mutableMapOf<Int,Int>()
        threats.putAll(castSliderThreatRay(square, color))
        threats.putAll(castKnightThreatRay(square, color))
        threats.putAll(castPawnThreatRay(square, color))
       // println(threats.keys)
        return threats
    }
    private fun castCastleRay(square: Int, color: Int): MutableMap<Int, Int> {
        return castRay(square, color, castlingRayInstructions)
    }

    private fun castPawnThreatRay(square: Int, color: Int): MutableMap<Int, Int> {
        return castRay(square, color, pawnThreatRayInstructions)
    }
    private fun castSliderThreatRay(square: Int, color: Int): MutableMap<Int, Int> {
        return castRay(square,color, sliderThreatRayInstructions)
    }
    private fun castKnightThreatRay(square: Int, color: Int): MutableMap<Int, Int> {
        return castRay(square, color, knightThreatRayInstructions)
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
        holder: Holder<Int>,
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
                        if (sliderMatchesVector(targetPiece, vector)) continue
                    } else if (targetPiece.isKing()) {
                        yield(newSquare)
                        continue
                    }
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


    private fun crawlMoves(startSquare: Int, color: Int, moveInstructions: MoveInstructions): List<Int> {
        return crawlMovesFast(
            startSquare, color,
            moveInstructions.moveInfo, moveInstructions.moveFilter,
            moveInstructions.specialCondition)
    }

    fun crawlMovesFast(
        startSquare: Int,
        color: Int,
        moveInfo: MovementInfo,
        moveFilter: (Int, Int, Int) -> Boolean,
        specialCondition: Int = -1
    ): List<Int> {
        val result = mutableListOf<Int>()

        val vectors = moveInfo.getVectors()

        for (vector in vectors) {
            val tracker = mutableListOf<Int>()
            var distance = 0

            for (step in 1..min(moveInfo.distance, BOARD_WIDTH)) {
                val newSquare = startSquare + (step * vector)
                val pastSquare = startSquare + ((step - 1) * vector)
                val targetPiece = getPiece(newSquare)
                distance += 1

                // wrapping is never allowed.
                if (doesSliderCrawlerWrap(startSquare, newSquare, pastSquare)) break

                // move must pass
                if (!moveFilter(startSquare, newSquare, pastSquare)) break

                when (specialCondition) {
                    XRAY_CONDITION -> {
                        if (targetPiece.isColor(color)) {
                            tracker.add(newSquare)
                            if (tracker.size > 1) break
                            result.add(newSquare)
                            continue
                        }
                    }
                    ILLEGAL_CAPTURES -> {
                        if (targetPiece.isColor(color)) {
                            result.add(newSquare)
                            if (sliderMatchesVector(targetPiece, vector)) continue
                        } else if (targetPiece.isKing()) {
                            result.add(newSquare)
                            continue
                        }
                    }
                }

                if (targetPiece.isOccupied()) {
                    if (collisionIsCapture(newSquare, color)) {
                        result.add(newSquare)
                    }
                    break
                }

                result.add(newSquare)
            }
        }

        return result
    }

    private fun crawlPawnMoves(startSquare: Int, piece: Piece, illegalCaptures: Int = -1): MutableSet<Int> {
        val validSquares = mutableSetOf<Int>()
        var canJump = false

        for (vector in pawnMoveInfo.getVectors()) {
            val newSquare = startSquare + (vector * getPawnDirection(piece.color))


            if ((isDiagonalVector(vector) && (isValidPawnCapture( startSquare, newSquare, piece.color))
                        || isEnpassantCapture(newSquare, piece.color)) || illegalCaptures == ILLEGAL_CAPTURES) {
                validSquares.add(newSquare)
            }

            if (isVerticalVector(vector) && isEmptySquare(newSquare)) {
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





    private fun doesSliderCrawlerWrap(origin: Int, endSquare: Int, pastSquare: Int): Boolean =
        (isOnSide(origin) && isOnSide(endSquare) && doesWrap(pastSquare, endSquare)
                || !isOnSide(origin) && isOnSide(endSquare) && isOnSide(pastSquare))

    private fun isValidSliderMove(origin: Int, endSquare: Int, pastSquare: Int): Boolean {
        return isInBounds(endSquare) && !doesSliderCrawlerWrap(origin, endSquare, pastSquare)
    }
    private fun isValidLeaperMove(origin: Int, endSquare: Int, color: Int,): Boolean {
        return isInBounds(endSquare) &&  (doesNotWrap(origin, endSquare)
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
                && (endSquare - origin).sign == getPawnDirection(color)
                && doesNotWrap(origin, endSquare)
                && isDiagonalMove(origin, endSquare)

    }
    private fun isEnpassantCapture(square: Int, color: Int): Boolean {
        return getSquareBehind(square, color) == referenceBoard.fetchEnpassantSquare()
    }


    private fun isDiagonalVector(vector: Int): Boolean {
        return abs(vector) == 7 || abs(vector) == 9
    }
    private fun isLineVector(vector: Int): Boolean {
        return abs(vector) == 8 || abs(vector) == 1
    }

    private fun isKnightVector(vector: Int): Boolean {
        return abs(vector) == 6 || abs(vector) == 10 || abs(vector) == 15 || abs(vector) == 17
    }
    private fun isVerticalVector(vector: Int): Boolean {
        return abs(vector) % BOARD_WIDTH == 0;
    }

    private fun vectorBetween(origin: Int, endSquare: Int): Int {
        val difference = endSquare - origin
        val vectorBetween = when  {
            difference < 0 -> when {
                rowDistance(origin, endSquare) == 0 -> -1
                difference % 8 == 0 -> -8
                difference % 9 == 0 -> -9
                difference % 7 == 0 -> -7
                else -> difference
            }
            difference > 0 -> when {
                rowDistance(origin, endSquare) == 0 -> 1
                difference % 8 == 0 -> 8
                difference % 9 == 0 -> 9
                difference % 7 == 0 -> 7
                else -> difference
            }
            else -> 0
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

    fun getCheckedKing(color: Int): Int {
        kingSquareHolder.hold(-1)
        val kingSquare = referenceBoard.getKingPosition(color)
        require(kingSquare != null) {"King Square cannot be null."}
            val kingDefendingSquares = castThreatRay(kingSquare, color)
            if (kingDefendingSquares.isNotEmpty()) {
                kingSquareHolder.hold(kingSquare)
            }

        return kingSquareHolder.show() ?: -1

    }

    private fun getAllPieces(color: Int = NO_COLOR): MutableMap<Int, Piece> {
        return referenceBoard.fetchPieces(color)
    }
    fun setReferenceBoard(board: Board) {
        referenceBoard = board
    }

    // todo: fix board rendering and update structures to use pieces and bit boards
    // todo: implement the BoardUtils.pieceCode encoder
}