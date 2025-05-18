package engines

import BoardUtils.Holder
import BoardUtils.move
import Base.Board
import Base.Game
import player.Player
import kotlin.random.Random

abstract class Engine : Player {

    abstract fun prepare(team: Int, game: Game)

    //abstract fun makePlay(timeLimit: Long, board: Board, receiver: Holder<Int>)

    fun getRandom(moves: List<move>): move {
        return if (moves.size - 1 == 0 ) moves[0] else moves[Random.nextInt(0, moves.size)]
    }
    abstract fun playMove(receiver: Holder<move>)
}