package player

import Base.MoveGenerator

interface Player {
    fun getName(): String
    fun playMove()
}