package Base

import BoardUtils.*
import engines.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.reflect.KClass


class Game {
    companion object {
        private const val INVALID_ID = -1
        private val PLAYER_LIST: List<KClass<out player.Player>> = listOf(
            Capturer::class,
            CapturerWithCheck::class,
            MasterOfZeroDepth::class,
            RandomMover::class
        )
        fun getRandomPlayer(): KClass<out player.Player> {
            return PLAYER_LIST[Random.nextInt(0, PLAYER_LIST.size)]
        }
        fun getRandomPlayerInstance(): player.Player {
            return getRandomPlayer().constructors.first().call()
        }
         enum class Status {
            UNREADY,
            READY_TO_BEGIN,
            ONGOING,
            FINISHED
         }
         enum class Player {
             WHITE,
             BLACK,
         }
    }

    object Sessions {
        private const val MAX_SESSIONS = 8
        private const val EMPTY_SESSION = 0L
        private val sessionBuffer = LongArray(MAX_SESSIONS) { EMPTY_SESSION }

        fun registerNew(): Int {
            var id = 0
            while (id < MAX_SESSIONS) {
                if (sessionBuffer[id] == EMPTY_SESSION) {
                    sessionBuffer[id] = newToken()
                    return id
                }
                id++
            }
            return INVALID_ID
        }

        fun vacate(id: Int) {
            enforceInBounds(id)
            sessionBuffer[id] = EMPTY_SESSION
        }

        fun vacateAll() {
            sessionBuffer.fill(EMPTY_SESSION)
        }

        fun isSessionValid(id: Int, token: Long): Boolean {
            enforceInBounds(id)
            return sessionBuffer[id] == token
        }

        fun isLegalID(id: Int): Boolean {
            return id in 0.until(MAX_SESSIONS)
        }

        fun getToken(id: Int): Long {
            enforceInBounds(id)
            return sessionBuffer[id]
        }

        private fun newToken(): Long {
            return Random.nextLong()
        }

        private fun enforceInBounds(id: Int) {
            if (id >= MAX_SESSIONS) throw IllegalArgumentException("Invalid Session ID: $id. ID must be between 0 and ${MAX_SESSIONS - 1}")
        }
    }

    private val player1: player.Player
    private val player2: player.Player
    private var player1color = -1
    private var player2color = -1

    var board = Board()
    private var generator = MoveGenerator(board)

    private var turn = 0 // 0 = White, 1 = Black
    var status = Status.UNREADY
    private val receiver = Holder<move>()
    private var legalMovesForTurn = emptyList<move>()
    // register a new game, otherwise starting is disallowed.
    private val sessionID = Sessions.registerNew()
    private val sessionToken = if (Sessions.isLegalID(sessionID)) Sessions.getToken(sessionID)
    else throw IllegalStateException("Cannot create game session: session buffer is full.")

    constructor(player1: player.Player? = null, player2: player.Player? = null) {
        this.player1 = player1 ?: getRandomPlayerInstance()
        this.player2 = player2 ?: getRandomPlayerInstance()
        randomizeColors()
    }

    //repeat(1000) {board.moveGenerator.genAllLegalMoves(BLACK)}
    //repeat(10) {board.moveGenerator.benchmarkMovegen()}
    fun prepareToBegin(fen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1") {
        // get the game ready to begin
        board.loadBoard(fen)
        if (player1 is Engine) withValidity(sessionID, sessionToken) { player1.prepare(player1color, this) }
        if (player2 is Engine) withValidity(sessionID, sessionToken) { player2.prepare(player2color, this) }

        println(board.getBoardString(player1color))
        printBorder()
        println("New game started. ID: $sessionID")
        println(
            player1.getName() + " (" + colorsNames[player1color] + ") vs " +
            player2.getName() + " (" + colorsNames[player2color] + ")"
        )
        println("Turn: ${if (player1color == WHITE) player1.getName() else player2.getName() }")
        printBorder()
        status = Status.READY_TO_BEGIN
    }
    fun begin() {
        if (status != Status.READY_TO_BEGIN) return
        status = Status.ONGOING
        CoroutineScope(Dispatchers.Default).launch {
            //legalMovesForTurn = generator.genAllLegalMoves(turn).filter { it != 0 }
            while (status == Status.ONGOING && Sessions.isSessionValid(sessionID,sessionToken)) {

                if (turn == player2color && player2 is Engine) withValidity(sessionID, sessionToken) { player2.makePlayy(receiver) }
                if (turn == player1color && player1 is Engine) withValidity(sessionID, sessionToken) { player1.makePlayy(receiver) }
                printBorder()
                withValidity(sessionID, sessionToken) {
                    if (receiver.hasValueChanged()) {
                        actOnBoard()
                    }
                }

                delay(1400)
            }

            if (!Sessions.isSessionValid(sessionID, sessionToken)) {
                println("Session Terminated: $sessionID")
            }
        }

    }
    private fun actOnBoard() {
        generateTurnMoves()
        val move = receiver.drop() ?: throw IllegalArgumentException("Move cannot be null.")
        receiver.forget()

        require(move in legalMovesForTurn) { "Impossible move: ${move.getString()}" }
        board.makeMove(move)
        changeTurn()
        println(board.getBoardString(player1color))
    }

    private fun generateTurnMoves() {
        legalMovesForTurn = generator.genAllLegalMoves(turn).filter { it != 0 }
    }
    private fun printBorder() {
        println("--------------------------------------------------------------------")
    }

    private fun randomizeColors() {
        player1color = Random.nextInt(0, 2)
        player2color = when (player1color) {
            0 -> 1
            1 -> 0
            else -> throw IllegalStateException("Player 1 cannot be $player1color") // this should never happen
        }
    }

    private fun withValidity(id: Int, token: Long, action: () -> Unit) {
        if (Sessions.isSessionValid(id, token)) {
            action()
        }
    }

    fun receiveMove(move: move) {
        board.tryMove(move.start(), move.end())
        receiver.hold(move)
    }

    private fun isOver(): Boolean {
        return legalMovesForTurn.isEmpty() || isThreeRepetition() || fiftyMoveRule()
    }
    private fun isThreeRepetition(): Boolean {
        return false            // need to implement
    }
    private fun fiftyMoveRule(): Boolean {
        return false            // need to implement
    }

    private fun endgame(): Int {
        if (isOver()) {
            status = Status.FINISHED
        }
        return 0
    }
    /*
     todo: { rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
     w - turn to move
     0 1 - half move clock (adds to 100ply)
     */
    fun changeTurn(newTurn: Int = -1): Boolean {
        if (newTurn !in -1..1) throw IllegalStateException("Turn cannot be: $turn")
        if (newTurn == -1) {
            turn = when (turn) {
                WHITE -> BLACK
                BLACK -> WHITE
                else -> throw IllegalStateException("Turn cannot be: $turn")
            }
        } else {
            turn = newTurn
            return true
        }
        return false
    }

}




