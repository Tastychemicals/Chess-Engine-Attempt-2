package engines

import BoardUtils.Holder
import BoardUtils.move
import Base.Board
import Base.Game

interface Engine {

    fun prepare(team: Int, game: Game)

    fun start()
    fun makePlay(timeLimit: Long, board: Board, receiver: Holder<Int>)

    fun getName(): String
    fun makePlayy(receiver: Holder<move>)
}