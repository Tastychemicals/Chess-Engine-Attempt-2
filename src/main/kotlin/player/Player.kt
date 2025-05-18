package player

import Base.MoveGenerator
import BoardUtils.Holder
import BoardUtils.move

interface Player {
    fun getName(): String
    fun getColor(): Int
    //fun playMove(receiver: Holder<move>)
}