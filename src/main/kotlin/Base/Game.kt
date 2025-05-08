package Base

import BoardUtils.BLACK
import BoardUtils.Holder
import BoardUtils.WHITE
import BoardUtils.getString
import BoardUtils.move
import Base.Game.Sessions.isSessionExpired
import BoardUtils.CASTLE_MOVE_DISTANCE
import BoardUtils.Move
import BoardUtils.end
import BoardUtils.isDiagonalMove
import BoardUtils.isOnBack
import BoardUtils.start
import UI.Visualizer
import engines.Engine
import engines.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random


class Game {

    object players {
        var isUserGame = false
        var isTwoUsersGame = false

        val player1: Engine = CapturerWithCheck()
        val player2: Engine = Capturer()

        var player1color = -1
        var player2color = -1

        fun randomizeColors() {
            player1color = Random.nextInt(0,2)
            player2color = when (player1color) {
                0 -> 1
                1 -> 0
                else -> throw IllegalStateException("Player 1 cannot be $player1color") // this should never happen
            }
        }

    }
    var board = Board()
    var generator = MoveGenerator(board)
    var turn = -1 // 0 = White, 1 = Black
    var isOngoing = false
    val receiver = Holder<move>()
    val game = this
    var visualizer = Visualizer(this) // fix and make beter here
    var legalMovesForTurn = emptyList<move>()

    object Sessions {
        var killSession = false
        var token = Holder<Any>()
        var count = 0
//        suspend fun killOtherSessions() {
//            //CoroutineScope(Dispatchers.Default).launch{
//                killSession = true
//                delay(20)
//                killSession = false
//            //}
//        }
        fun killAllSessions() {
            token = token.clone()
        }
        fun isSessionExpired(token: Holder<Any>): Boolean {
            return token !== Sessions.token
        }
    }
    fun setNewVisualizer(vis: Visualizer) {
        this.visualizer = vis
    }

    fun startNewGame(fen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1") {

        CoroutineScope(Dispatchers.Default).launch {
            Sessions.killAllSessions()
            val sessionToken = Sessions.token
            Sessions.count++

            turn = 0 // this is a test text
            board = Board()
            generator = MoveGenerator(board)
            isOngoing = true


            board.loadBoard(fen)
            //repeat(1000) {board.moveGenerator.genAllLegalMoves(BLACK)}
            //repeat(10) {board.moveGenerator.benchmarkMovegen()}


            println(board.getBoardString(players.player1color))
            println("New Game Started.")
            println("Turn: WHITE")

            println(
                if (players.isTwoUsersGame)
                    "Player vs Player" else if (players.isUserGame)
                    "Player vs ${players.player2.getName()}" else
                        players.player1.getName() + " vs " + players.player2.getName()
            )



            var clock = 0

            // get the game ready to begin
            players.randomizeColors()
            players.player1.prepare(players.player1color, game)
            players.player2.prepare(players.player2color, game)
            visualizer.setNewOrientation(players.player1color)

            //legalMovesForTurn = generator.genAllLegalMoves(turn).filter { it != 0 }
            while (isOngoing && !isSessionExpired(sessionToken)) {

                clock++
                if (receiver.hasValueChanged()) {
                    legalMovesForTurn = generator.genAllLegalMoves(turn).filter { it != 0 }
                    tryMove()
                }

                // handle the 3 settings: PVP, PVE, EVE
                if (!players.isTwoUsersGame) {
                    if (turn == players.player2color) players.player2.makePlayy(receiver)
                    if (!players.isUserGame) if (turn == players.player1color) players.player1.makePlayy(receiver)
                }
                //if (clock == 500) { println("ongoing..."); clock = 0}
                delay(800)
            }

            if (isSessionExpired(sessionToken)) {
                println("session terminated")
            }
            Sessions.count--
        }






    }

    fun tryMove() {
        val move = receiver.drop()?: throw IllegalArgumentException("Move cannot be null.")
        receiver.forget()

        require (move in legalMovesForTurn) {"Impossible move: ${move.getString()}"}
            board.makeMove(move)
            changeTurn()
            visualizer.updateCustomMask()
            println(board.getBoardString(players.player1color))
    }

    fun runGame() {

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




