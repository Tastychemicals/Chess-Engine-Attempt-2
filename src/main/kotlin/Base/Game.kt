package Base

import Base.Game.Sessions.isExpired
import BoardUtils.*
import engines.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import player.Player
import kotlin.math.abs
import kotlin.random.Random
import kotlin.random.nextLong
import kotlin.reflect.KClass


class Game {
    companion object {
        val PLAYER_LIST = listOf(
            Capturer::class,
            CapturerWithCheck::class,
            MasterOfZeroDepth::class,
            RandomMover::class
        )
    }

    private object Sessions {
        private var currentToken = Random.nextLong()
        fun killAllSessions() {
            currentToken = Random.nextLong()
        }
        fun getCurrentToken(): Long {
            return currentToken
        }
        fun isExpired(token: Long): Boolean {
            return token != currentToken
        }
    }
    private val player1: Player
    private val player2: Player
    private var player1color = -1
    private var player2color = -1
    private var token = Sessions.getCurrentToken()

    var board = Board()
    var generator = MoveGenerator(board)

    var turn = 0 // 0 = White, 1 = Black
    var isOngoing = false
    val receiver = Holder<move>()
    var legalMovesForTurn = emptyList<move>()

    constructor(player1: Player? = null, player2: Player? = null) {
        this.player1 = player1?: getRandomPlayer().constructors.first().call()
        this.player2 = player2?: getRandomPlayer().constructors.first().call()
        randomizeColors()
    }
    private fun getRandomPlayer(): KClass<out Player> {
        return PLAYER_LIST[Random.nextInt(0, PLAYER_LIST.size)]
    }

    private fun randomizeColors() {
        player1color = Random.nextInt(0,2)
        player2color = when (player1color) {
            0 -> 1
            1 -> 0
            else -> throw IllegalStateException("Player 1 cannot be $player1color") // this should never happen
        }
    }


fun startNewGame(fen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1") {
    token = Sessions.getCurrentToken()
    board.loadBoard(fen)
            //repeat(1000) {board.moveGenerator.genAllLegalMoves(BLACK)}
            //repeat(10) {board.moveGenerator.benchmarkMovegen()}
// get the game ready to begin
    if (player1 is Engine) player1.prepare(player1color, this)
    if (player2 is Engine) player2.prepare(player2color, this)


    println(board.getBoardString(player1color))
    printBorder()
    println("New Game Started.")
    println("Turn: WHITE")

            println(player1.getName() + " (" + colorsNames[player1color] + ") vs " +
                    player2.getName() + " (" + (colorsNames[player2color] + ")"))
    printBorder()

            var clock = 0


    CoroutineScope(Dispatchers.Default).launch {
            //legalMovesForTurn = generator.genAllLegalMoves(turn).filter { it != 0 }
        isOngoing = true
        while (isOngoing && !Sessions.isExpired(token)) {

                clock++

                // handle the 3 settings: PVP, PVE, EVE
                if (!isTwoUsersGame) {
                    printBorder()
                    if (turn == player2color) player2.makePlayy(receiver)
                    if (!isUserGame) if (turn == player1color) player1.makePlayy(receiver)
                    printBorder()
                }
                if (receiver.hasValueChanged()) {
                    legalMovesForTurn = generator.genAllLegalMoves(turn).filter { it != 0 }
                    tryMove()
                }

                //if (clock == 500) { println("ongoing..."); clock = 0}
                delay(1400)
            }

            if (isExpired(token)) {
                println("session terminated")
            }
        }

    }

    fun tryMove() {
        val move = receiver.drop()?: throw IllegalArgumentException("Move cannot be null.")
        receiver.forget()

        require (move in legalMovesForTurn) {"Impossible move: ${move.getString()}"}
            board.makeMove(move)
            changeTurn()
            println(board.getBoardString(player1color))
    }

    fun runGame() {

    }

    fun printBorder() {
        println("--------------------------------------------------------------------")
    }


    fun receiveMove(move: move) {
        board.tryMove(move.start(), move.end())
        receiver.hold(move)
    }

    fun receiveMove(origin: Int, endSquare: Int) {
        val piece = board.fetchPiece(origin)
        val pieceOnEnd = board.fetchPiece(endSquare)       // this handling is horrible but gets the job done for now
        var flags = Move.encodeFlags(
            pieceOnEnd.isOccupied(),
            (piece.isKing() && abs(origin - endSquare) == CASTLE_MOVE_DISTANCE),
            (piece.isPawn() && isOnBack(endSquare)),
            (piece.isPawn() && pieceOnEnd.isEmpty() && isDiagonalMove(origin, endSquare))
        )

        receiveMove(Move.encode(origin, endSquare, flags))
    }

    private fun endgame(): Int {
        isOngoing = false
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
            turn == newTurn
            return true
        }
        return false
    }

}




