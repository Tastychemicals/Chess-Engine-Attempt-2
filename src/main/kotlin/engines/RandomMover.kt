package engines

import BoardUtils.*

import Base.*
import Base.Game.Sessions

import kotlin.random.Random

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.io.println

class RandomMover : Engine {
    val moveGenerator = MoveGenerator(Board())
    var team = -1
    var board = Board()
    var game = Game()

    override fun prepare(team: Int, game: Game) {
        this.team = team
        this.game = game
        moveGenerator.setReferenceBoard(game.board)

    }
    override fun start() {
        CoroutineScope(Dispatchers.Default).launch {
//            while (!Sessions.isSessionExpired()) {
//                if (game.turn == team) {
//                    //makePlay()
//                }
//            }
        }
    }

    override fun makePlay(timeLimit:Long, board: Board,  receiver: Holder<Int>) {
        moveGenerator.setReferenceBoard(board)
        val moves= moveGenerator.genAllLegalMoves(team)
        val random: move = moves[Random.nextInt(0, moves.size - 1)]

        receiver.hold(random)
    }


    override fun getName(): String {
        return "Random Mover"
    }

    override fun makePlayy(receiver: Holder<move>) {

        val moves = moveGenerator.genAllLegalMoves(team).filter { it != 0 }
        val random = if (moves.size - 1 == 0 ) moves[0] else moves[Random.nextInt(0, moves.size - 1)]
        println("Trying: " + random.getString() )
        receiver.hold(random)

    }

}