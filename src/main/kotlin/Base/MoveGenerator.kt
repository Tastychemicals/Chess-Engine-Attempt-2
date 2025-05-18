package Base
import BoardUtils.*
import BoardUtils.isOnSide
import kotlin.collections.filter
import kotlin.collections.mutableListOf
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

typealias MoveFilter = (Int, Int, Int) -> Boolean
typealias alertTrigger = (Int, Int, Int, Piece, Int, MutableList<Int>) -> Boolean
typealias alertCondition = (Int, Int, Int, Piece) -> Boolean
typealias mappingInfo = (Int, Int, Int) -> Int

class MoveGenerator(board: Board) {
    //------------------------------------------------------------------------------------------------------------------
    /**
                                    Move Information (how should the crawler move?)
    */

    private val pawnMoveInfo = MovementInfo(7, 8, 9, 16, pieceType = PAWN)
    private val knightMoveInfo = MovementInfo(-17, -15, -10,-6, 6, 10, 15, 17, pieceType = KNIGHT, maxDistance = 1)
    private val bishopMoveInfo = MovementInfo(-9, -7, 7, 9, pieceType = BISHOP)
    private val rookMoveInfo = MovementInfo(-8, -1, 1, 8, pieceType = ROOK)
    private val queenMoveInfo = MovementInfo(-9, -8, -7, -1, 1, 7, 8, 9, pieceType = QUEEN)
    private val kingMoveInfo = MovementInfo(-9, -8, -7, -1, 1, 7, 8, 9, pieceType = KING, maxDistance = 1)


    private val noMoveInfo = MovementInfo(pieceType = EMPTY)
    private val pawnAttackInfo = MovementInfo(7,9, -7, -9, pieceType = BISHOP, maxDistance = 1)
    private val castlingMoveInfo = MovementInfo(-1,1, pieceType = ROOK, maxDistance = 4)
    private val horizontalMoveInfo = MovementInfo(-1, 1, pieceType = 4)
    //------------------------------------------------------------------------------------------------------------------

    /**
                                 Move Filters (what moves does the crawler validate?)
     */
    val sliderMoveFilter = { startSquare: Int, newSquare: Int, pastSquare: Int -> isValidSliderMove(startSquare, newSquare, pastSquare) }
    val leaperMoveFilter = { startSquare: Int, newSquare: Int, pastSquare: Int -> isValidLeaperMove(startSquare, newSquare, pastSquare) }
    val emptySquareFilter = { startSquare: Int, newSquare: Int, pastSquare: Int -> getPiece(newSquare).isEmpty() }

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
    val CHECK_DETECT = 3
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

    val noInstructions =                MoveInstructions(noMoveInfo, leaperMoveFilter)
    val kingXrayInstructions =          MoveInstructions(queenMoveInfo, pinRayFilter, XRAY_CONDITION)
    val pawnAttackCheckInstructions =   MoveInstructions(pawnAttackInfo, pawnAttackFilter)
    val castlingInstructions =          MoveInstructions(castlingMoveInfo, sliderMoveFilter, ILLEGAL_CAPTURES)

    val sliderCheckInstructions =       MoveInstructions(queenMoveInfo, sliderMoveFilter, CHECK_DETECT)
    val knightCheckInstructions =       MoveInstructions(knightMoveInfo, leaperMoveFilter, CHECK_DETECT)

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
    val AS_IS_RAY = 4        // does not judge the crawler
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

        (piece.isColor(color)  // must be team
                && piece.isRook() && piece.hasMoved) // add you because the size of the alerts will be too large
                || (!piece.isColor(color) && piece.isEmpty() && distance <= KING_CASTLE_DISTANCE)
                || piece.isOccupied() && !piece.isRook()

    }
    val addAllCondition: alertCondition = {color: Int, square: Int, distance, piece: Piece -> true}
    //val collisionCondition: alertCondition = {color: Int, square: Int, distance, piece: Piece -> true}

    val pieceIsPawnCondition: alertCondition = {color: Int, _, _, piece: Piece -> pieceIsEnemyCondition(color, NO_PARAM, NO_PARAM, piece) && piece.isPawn()}
    val pieceIsKnightCondition: alertCondition = {color: Int, _, _, piece: Piece ->  pieceIsEnemyCondition(color, NO_PARAM, NO_PARAM, piece) && piece.isKnight()}
    //------------------------------------------------------------------------------------------------------------------

    /**
                                    Ray Triggers (when should the ray return data?)
     */
    val pinTrigger: alertTrigger = { color: Int, square: Int, _, piece: Piece, vector: Int, alerts: MutableList<Int> -> sliderMatchesVector(piece, vector) && !piece.isColor(color) }
    val sliderThreatTrigger: alertTrigger = { color: Int, square: Int, _, piece: Piece, vector: Int, alerts: MutableList<Int> -> sliderMatchesVector(piece, vector) && !piece.isColor(color) }
    val alertFoundTrigger: alertTrigger = { color: Int, square: Int, _, piece: Piece, vector: Int, alerts: MutableList<Int>  -> alerts.isNotEmpty() }
    // ONLY WORKS WITH CUMULATING RAYS.

    val collisionFoundTrigger: alertTrigger = { color: Int, square: Int, endSquare: Int, piece: Piece, vector: Int, alerts: MutableList<Int> -> getPiece(endSquare).isOccupied() || isOnSide(endSquare) || isOnBack(endSquare)}
    val pieceFoundTrigger: alertTrigger = { color: Int, square: Int,_,  piece: Piece, vector: Int, alerts: MutableList<Int> ->
        piece.isOccupied()
    }
    //------------------------------------------------------------------------------------------------------------------

    /**
     *                                  Ray trail Info (What should the trail add?)
     */
    val vectorTrailInfo: mappingInfo = { currentSquare: Int, distance: Int, vector: Int  -> vector}
  // val sameSquareTrailInfo: mappingInfo = { currentSquare: Int, distance: Int, vector: Int  -> }
    //------------------------------------------------------------------------------------------------------------------
    /**
                                    Ray Instructions (how should the ray cast?)
     */
    val reverseKnightAttackRayInstructions = RayInstructions(knightMoveInstructions, addAllCondition, collisionFoundTrigger, NORMAL_RAY)
    val reverseSliderAttackRayInstructions = RayInstructions(queenMoveInstructions, addAllCondition, collisionFoundTrigger, NORMAL_RAY)

    val pinRayInstructions = RayInstructions(kingXrayInstructions, pinCondition, pinTrigger, MAPPING_RAY, vectorTrailInfo)
    val sliderThreatRayInstructions = RayInstructions(queenMoveInstructions, sliderThreatCondition, sliderThreatTrigger, NORMAL_RAY)
    val knightThreatRayInstructions = RayInstructions(knightMoveInstructions, pieceIsKnightCondition, alertFoundTrigger, NORMAL_RAY)
    val pawnThreatRayInstructions = RayInstructions(pawnAttackCheckInstructions, pieceIsPawnCondition, alertFoundTrigger, NORMAL_RAY)
    val castlingRayInstructions = RayInstructions(castlingInstructions, castlingCondition, pieceFoundTrigger, CASTLE_RAY)
    //val potentialCheckRayInstructions = RayInstructions(potentialCheckInstructions, addAllCondition, edgeTouchedTrigger)
    //------------------------------------------------------------------------------------------------------------------

    private var referenceBoard = board


     var enemyAttackSquares = IntArray(64)
    var enemyAttackBitBoard = 0L
    private var attackedKing = -1
    private var kingSquare: Int? = null
    private var enemyKingSquare: Int? = null

    private var kingDefendingSquares = mutableMapOf<Int,Int>()
    var knightCheckSquares = 0L
    var sliderCheckSquares = 0L
    private var pins = mutableMapOf<Int,Int>()
    private var moveBuffer = IntArray(265)
    private var moveCount = 0
    private var moveFlags = 0

    var raysCasted = 0



    data class MoveInstructions(
        val moveInfo: MovementInfo,
        val moveFilter: MoveFilter,
        val specialCondition: Int = -1,
    )
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
        val maxDistance: Int = BOARD_AXIS_LENGTH,
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


    private fun IntArray.clear() {
        moveBuffer.fill(0)
        moveCount = 0
    }
    private fun addMove(move: move) {
        moveBuffer[moveCount++] = move
    }
    private fun remove() {

    }


    /**
     * the main outlet called in board
     */
    fun genAllLegalMoves(color: Int): IntArray {
        var totalMoves = 0
        val pieces = getAllPieces(color)
        moveBuffer.clear()
        //val start = measureNanoTime {
        kingSquare = getKingSquare(color)
        enemyKingSquare = getKingSquare(getOppositeColor(color))
        require (kingSquare != null) {"${typeNames[color]} King does not exist"}
        // require (enemyKingSquare != null) {"${typeNames[color]} King does not exist"}
        raysCasted = 0

        pins = castPinRay(kingSquare!!, color)
        enemyAttackSquares = getOpponentAttackSquares(color)
        kingDefendingSquares = castThreatRay(kingSquare!!, color)
        knightCheckSquares = getKnightCheckSquares(color)
        sliderCheckSquares = getSliderCheckSquares(color)
        attackedKing = getCheckedKing()




        for (square in pieces.indices) {
            val piece = getPiece(square, color)
            if (piece.isEmpty()) continue
            val moves: Long = genLegalPieceMoves(square, piece)
            //println("Showing moves for $piece")
            //printBitboard(moves)

            for (endSquare in 0.until(BOARD_SIZE)) {
                if (moves and (1L shl endSquare) != 0L) {

                    val flags = getMoveFlags(square, endSquare)
                    val move: move = Move.encode(square, endSquare, flags)
                    if (move.isPromotion()) {
                        val fromPromotion = true
                        val move1 = (Move.addFlags(move, Move.addPromotionType(flags, QUEEN)))
                        val move2 =(Move.addFlags(move, Move.addPromotionType(flags, KNIGHT)))
                        val move3 = (Move.addFlags(move, Move.addPromotionType(flags, ROOK)))
                        val move4 = (Move.addFlags(move, Move.addPromotionType(flags, BISHOP)))


                        addMove(Move.addFlags(move1, Move.addIfCheck(flags, isMoveCheck(makePiece(color,move1.getPromotion(), true), endSquare, fromPromotion))))
                        addMove(Move.addFlags(move2, Move.addIfCheck(flags, isMoveCheck(makePiece(color,move2.getPromotion(), true), endSquare, fromPromotion))))
                        addMove(Move.addFlags(move3, Move.addIfCheck(flags, isMoveCheck(makePiece(color,move3.getPromotion(), true), endSquare, fromPromotion))))
                        addMove(Move.addFlags(move4, Move.addIfCheck(flags, isMoveCheck(makePiece(color,move4.getPromotion(), true), endSquare, fromPromotion))))
                    } else {
                        addMove(move)
                    }
                    totalMoves++
                }
            }
        }
        //println(totalMoves)
        //}
        //println("rays casted on iteration: $raysCasted with a time of: ${start/1_000.0}micros")
        println("Total moves found: $totalMoves")
        println("Total moves in buffer: ${getMoveBufferFilledSpaces()}")
        return moveBuffer
    }

    fun getMoveFlags(origin: Int, endSquare: Int): Int {

        val piece = getPiece(origin)
        val pieceOnEnd = getPiece(endSquare)
        val moveIsCheck = isMoveCheck(origin, endSquare)
        val flags = Move.encodeFlags(
            pieceOnEnd.isOccupied(),
            (piece.isKing() && abs(origin - endSquare) == CASTLE_MOVE_DISTANCE),
            (piece.isPawn() && isOnBack(endSquare)),
            (piece.isPawn() && pieceOnEnd.isEmpty() && isDiagonalMove(origin, endSquare)),
            moveIsCheck
        )
        return flags
    }
    fun isMoveCheck(startSquare: Int, endSquare: Int): Boolean {
        val piece = getPiece(startSquare)
        return isMoveCheck(piece, endSquare)
    }

    fun isMoveCheck(piece: Piece, endSquare: Int, fromPromotion: Boolean = false): Boolean {
        if (piece.isEmpty() || piece.isKing()) return false
        if (enemyKingSquare == null) enemyKingSquare = referenceBoard.getKingPosition(piece.fetchColor())
        val vector = vectorBetween(endSquare, enemyKingSquare as Int)
        val pawnDirection = getPawnDirection(piece.fetchColor())
        return when {
            piece.isSlider() -> {
                if ((1L shl endSquare) and sliderCheckSquares != 0L && sliderMatchesVector(piece, vector)) {
                    if (rowIs(endSquare, getPromotionRank(piece.fetchColor()))) {
                        (fromPromotion || !getPiece(endSquare - 8 * pawnDirection).isColor(piece.fetchColor()))
                    } else true
                } else false
            }
            piece.isPawn() -> isDiagonalVector(vector)
                    && vector.sign == pawnDirection.sign
                    && colDistance(endSquare, enemyKingSquare as Int) == 1
            piece.isKnight() -> (1L shl endSquare) and knightCheckSquares != 0L
            else -> false
        }
    }


    fun getMoveBufferFilledSpaces(): Int {
        var count = 0
        for (move in moveBuffer) {
            if (move != 0) {
                count++
            }
        }
        return count
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
    // you never want to call this if you want speed.
    fun genLegalPieceMoves(square: Int): Long {
        val piece = getPiece(square)
        enemyAttackSquares = getOpponentAttackSquares(piece.fetchColor())
        return genLegalPieceMoves(square, piece)
    }
    private fun genLegalPieceMoves(square: Int, piece: Piece): Long {
        if (kingSquare == null) kingSquare = getKingSquare(piece.color)

        val disjointMoves = genPseudoPieceMoves(square, piece)
        var moves = 0L
        for (move in disjointMoves) {
            moves = moves or move
        }

        if (piece.isKing()) {
            moves = validateKingMoves(moves)
            moves = addCastleMoves(piece, square, moves)
        }else  {
            moves = filterOutPins(square, moves, pins)
            moves = filterKingDefense(moves)
        }

        return moves
    }

    fun printBitboard(bitboard: Long) {
        for (rank in 7 downTo 0) {
            for (file in 0..7) {
                val square = rank * 8 + file
                val bit = (bitboard shr square) and 1L
                print(if (bit == 1L) "1 " else ". ")
            }
            println("  ${rank + 1}")
        }
        println("a b c d e f g h\n")
    }

    private fun validateKingMoves(moves: Long): Long {
        var m = moves

        for (i in 0.until(64)) {
            if (enemyAttackSquares[i] > 0) {
                //println(enemyAttackSquares.joinToString())
                m = m and (1L shl i).inv()
                //printBitboard(m)
            }
        }

        return m
    }
    private fun addCastleMoves(piece: Piece,square: Int, moves: Long): Long {
        var m = moves
        if (!piece.hasMoved && attackedKing != square) {

            val r = castCastleRay(square, piece.color).keys
            for (endSquare in r) {
                m = m or (1L shl endSquare)
            }

        }
        return m
    }

    private fun filterOutPins(square: Int, moves: Long, pins: Map<Int, Int>): Long {
            if (square !in pins) return moves

            val allowedVector = pins[square]!!
            var filteredMoves = 0L

            for (target in 0 until 64) {
                val isMoveSet = ((moves shr target) and 1L) != 0L
                if (!isMoveSet) continue

                val v = vectorBetween(square, target)
                if (abs(v) == abs(allowedVector)) {
                    filteredMoves = filteredMoves or (1L shl target)
                }
            }

            return filteredMoves

    }
    private fun filterKingDefense(moves: Long): Long {
        return if (enemyAttackSquares[kingSquare as Int] > 1)  0L else
         if (enemyAttackSquares[kingSquare as Int] > 0) filterDefense(moves, kingDefendingSquares.keys)
         else moves
    }
    private fun filterDefense(moves: Long, defendingSquares: MutableSet<Int>): Long {

       return if (defendingSquares.isNotEmpty()) {

            var filtered = 0L
            for (square in defendingSquares) {
                if (((moves shr square) and 1L) != 0L) {
                    filtered = filtered or (1L shl square)
                    //if (square == 46) printBitboard(filtered)
                }
            }
           filtered
       } else moves
    }

    fun genPseudoPieceMoves(square: Int, piece: Piece): LongArray {               /// dont detele
        return getMoves(square, piece)
    }


    // gets the sum of all pseudo attack squares of the color's opponent (for king safety check)
    fun getOpponentAttackSquares(color: Int): IntArray {
        val attackMap = IntArray(64)  // way faster than mutableMap
        val enemyColor = getOppositeColor(color)
        val pieces = getAllPieces(enemyColor)
        //println("${referenceBoard.fetchPieces(getOppositeColor(color)).joinToString()}, ->  ${colorsNames[getOppositeColor(color)]} ")
        for (square in pieces.indices) {
            val piece = getPiece(square, enemyColor)
            if (piece.isOccupied()) {
                val attackMasks = if (piece.isPawn()) {
                    crawlPawnMoves(square, piece, ILLEGAL_CAPTURES, onlyCaptures = true)
                } else {
                    crawlMovesFast(
                        square,
                        piece.color,
                        getMovementInfo(piece.type),
                        getMoveFilter(piece.type),
                        ILLEGAL_CAPTURES,
                    )
                }

                for (mask in attackMasks) {
                    var m = mask
                    var position = 0
                    while (m != 0L && position < BOARD_SIZE) {

                        val bit = m and 1L
                        if (bit != 0L) {
                           // if ()
                            attackMap[position]++
                        }
                        position++
                        m = m shr 1

                    }
                }
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
        val rays = crawlMoves(startSquare, color, instructions)
        var distance: Int

        raysCasted++



        var vector: Int
        var direction = 0
        val vectors = instructions.moveInfo.getVectors()
        for (ray in rays) {
            alerts.clear()
            distance = 1
            if (direction >= vectors.size) break
            vector = vectors[direction]
            direction++

            while (distance < BOARD_AXIS_LENGTH) {
                var nextSquare = startSquare + (distance * vector)
                if ((1L shl nextSquare) and ray == 0L)  {
                    distance++
                    continue
                }

                val targetPiece = getPiece(nextSquare)

                if (alertCondition(color, startSquare, distance, targetPiece)) {     //alert trigger
                    alerts.add(nextSquare)
                }

                if (alertTrigger(color, startSquare, nextSquare, targetPiece, vector, alerts)) {
                    when (rayType) {
                        MAPPING_RAY -> for (square in alerts) {
                            found.put(square, mappingInfo(square, distance, vector))
                        }
                        NORMAL_RAY ->  {
                            found.putAll( format(alerts) )
                            found.put(startSquare, startSquare)
                           // println("alerts found for this ray: ${alerts.joinToString()}")
                            // continue //break
                        }

                        CASTLE_RAY -> {
                            if (alerts.size != distance && alerts.size == 2 && alerts.all {  enemyAttackSquares[it] == 0}) {
                                found.putAll(format(alerts))
                            }

                        }
                    }
                }
                distance++
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
       // println("Threats: " + threats.keys.joinToString())
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
    fun getKnightCheckSquares(color: Int): Long {
        return flattenCrawler(crawlMoves(enemyKingSquare as Int, color, knightCheckInstructions))
    }
    fun getSliderCheckSquares(color: Int): Long {
        return flattenCrawler(crawlMoves(enemyKingSquare as Int, color, sliderCheckInstructions))
    }



    private fun getMoves(startSquare: Int, piece: Piece): LongArray {
        return if (piece.isPawn()) crawlPawnMoves(startSquare, piece)
        else crawlMoves(startSquare, piece.color, getMoveInstructions(piece.type))

    }

    private fun crawlMoves(startSquare: Int, color: Int, moveInstructions: MoveInstructions): LongArray {
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
    ): LongArray {

        val allDirections = LongArray(8)

        val vectors = moveInfo.getVectors()

        for (direction in vectors.indices) {
            var tracker = 0L
            var thisDirectionsMoves = 0L
            val vector = vectors[direction]
            for (step in 1..min(moveInfo.maxDistance, BOARD_AXIS_LENGTH)) {
                val newSquare = startSquare + (step * vector)
                val pastSquare = startSquare + ((step - 1) * vector)
                val targetPiece = getPiece(newSquare)

                // wrapping is never allowed.
                if (doesSliderCrawlerWrap(startSquare, newSquare, pastSquare)) break

                // move must pass
                if (!moveFilter(startSquare, newSquare, pastSquare) || startSquare == newSquare) break

                when (specialCondition) {
                    XRAY_CONDITION -> {
                        if (targetPiece.isColor(color)) {
                            tracker = tracker or (1L shl newSquare)
                            if (tracker.countOneBits() > 1) break
                            thisDirectionsMoves = thisDirectionsMoves or (1L shl newSquare)
                            continue
                        }
                    }
                    ILLEGAL_CAPTURES -> {
                        if (targetPiece.isColor(color)) {
                            thisDirectionsMoves = thisDirectionsMoves or (1L shl newSquare)
                            if (sliderMatchesVector(targetPiece, vector)) continue
                        } else if (targetPiece.isKing()) {
                            thisDirectionsMoves = thisDirectionsMoves or (1L shl newSquare)
                            continue
                        }
                    }
                    CHECK_DETECT -> {
                        if (targetPiece.isColor(color)) {
                            //tracker = tracker or (1L shl newSquare)
                            //if (tracker.countOneBits() > 1) break
                            thisDirectionsMoves = thisDirectionsMoves or (1L shl newSquare)
                            if (sliderMatchesVector(targetPiece, vector)
                                || isOnSameCol( startSquare, newSquare)
                                && (targetPiece.isPawn()
                                        && getPawnDirection(color).sign == vector.sign
                                        && isBeforePromotionRank(newSquare, color))) {
                                continue
                            }
                        }
                    }
                }

                if (targetPiece.isOccupied()) {
                    if (!targetPiece.isColor(color)) {
                        thisDirectionsMoves = thisDirectionsMoves or (1L shl newSquare)
                    }
                    break
                }

                thisDirectionsMoves = thisDirectionsMoves or (1L shl newSquare)
            }
            allDirections[direction] = thisDirectionsMoves
        }

        return allDirections
    }

    private fun crawlPawnMoves(startSquare: Int, piece: Piece, illegalCaptures: Int = -1, onlyCaptures: Boolean = false): LongArray {
        var validSquares = 0L
        var canJump = false

        for (vector in pawnMoveInfo.getVectors()) {
            val newSquare = startSquare + (vector * getPawnDirection(piece.color))
            if (!isInBounds(newSquare)) continue


            if ((isDiagonalVector(vector)
                        && (((isValidPawnCapture( startSquare, newSquare, piece.color))
                        || isEnpassantCapture(newSquare, piece.color)) || (illegalCaptures == ILLEGAL_CAPTURES && !doesWrap(startSquare, newSquare))))) {
                validSquares = validSquares or (1L shl newSquare)
            }
            if (!onlyCaptures) {
                if (isVerticalVector(vector) && isEmptySquare(newSquare)) {
                    if (rowDistance(startSquare, newSquare) == 1) {
                        canJump = !isOnBack(newSquare)
                        validSquares = validSquares or (1L shl newSquare)
                    } else {
                        if (canJump && !piece.hasMoved) {
                            validSquares = validSquares or (1L shl newSquare)
                        }
                    }
                }
            }

        }
        return longArrayOf(validSquares)
    }

    fun flattenCrawler(rays: LongArray): Long {
        var map = 0L
        for (ray in rays) {
            map = map or ray
        }
        return map
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
        return abs(vector) % BOARD_AXIS_LENGTH == 0;
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


    private fun isLineMove(origin: Int, endSquare: Int): Boolean {
        return (rowDistance(origin, endSquare) == 0 && colDistance(origin, endSquare) != 0) ||
                (rowDistance(origin, endSquare) != 0 && colDistance(origin, endSquare) == 0)
    }

    private fun isEmptySquare(square: Int): Boolean {
        return getPiece(square).isEmpty()
    }
    private fun getPiece(square: Int, color: Int = NO_COLOR): Piece {
        return referenceBoard.fetchPiece(square, color)
    }
    private fun getKingSquare(color: Int): Int? {
        return referenceBoard.getKingPosition(color)
    }
    private fun isInBounds(square: Int): Boolean {
        return !(square >= BOARD_SIZE || square < 0)
    }

    fun getCheckedKing(color: Int = -1): Int {
        val whiteKingSquare = referenceBoard.getKingPosition(WHITE) as Int
        val blackKingSquare = referenceBoard.getKingPosition(BLACK) as Int
        //require(kingSquare != null) {"King Square cannot be null."}
            val whiteKingDefendingSquares = castThreatRay(whiteKingSquare, WHITE)
            val BlackKingDefendingSquares = castThreatRay(blackKingSquare, BLACK)
        return if (color != NO_COLOR) {
            when (color) {
                WHITE -> whiteKingSquare
                BLACK -> blackKingSquare
                else -> -1
            }
        } else if (whiteKingDefendingSquares.isNotEmpty()) {
            whiteKingSquare
        } else if (BlackKingDefendingSquares.isNotEmpty()) {
            blackKingSquare
        } else -1



    }

    private fun getAllPieces(team: Int = NO_COLOR): Array<Piece> {
        return referenceBoard.fetchPieces(team)
    }
    fun setReferenceBoard(board: Board) {
        referenceBoard = board
    }

    // todo: fix board rendering and update structures to use pieces and bit boards
    // todo: implement the BoardUtils.pieceCode encoder
}