package Game

import BoardUtils.BLACK
import kotlin.random.Random


class Game {
    var board: Board
    var turn: Int // 0 = White, 1 = Black
    var player1color: Int
    var player2color: Int
    var isOngoing: Boolean


    constructor() {
        board = Board(this)
        turn = -1
        player1color = 0
        player2color = 0
        isOngoing = false
    }

    fun startNewGame(fen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"): Boolean {
        turn = 0 // this is a test text
        board = Board(this)
        isOngoing = true
        player1color = Random.nextInt(0,2)
        player2color = when (player1color) {
            0 -> 1
            1 -> 0
            else -> { endgame() } // this should never happen
        }
       // println("Player.Player 1 color:" + player1color)
        //println("Player.Player 2 color:" + player2color)

        board.loadBoard(fen)
        println("new game started")
        println(board.getBoardString(player1color))

        //repeat(1000) {board.moveGenerator.genAllLegalMoves(BLACK)}
        //repeat(10) {board.moveGenerator.benchmarkMovegen()}


        return true
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
        if (newTurn !in -1..1) return false
        if (newTurn == -1) {
            turn = when (turn) {
                0 ->  1
                1 -> 0
                else -> return false
            }
        } else {
            turn == newTurn
            return true
        }
        return false
    }

}




