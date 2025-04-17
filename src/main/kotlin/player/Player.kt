package player

import Game.MoveGenerator

abstract class Player {
    abstract fun getName(): String
    abstract val moveGenerator: MoveGenerator
    abstract val evaluator: Evaluator
    class Listener() {

    }

}