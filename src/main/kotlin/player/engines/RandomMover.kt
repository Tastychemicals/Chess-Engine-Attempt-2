package engines

import BoardUtils.*

import Base.*
import Base.Game.Sessions

import kotlin.random.Random

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.io.println

class RandomMover : Engine() {
    val moveGenerator = MoveGenerator(Board())
    var team = -1
    var board = Board()
    var game = Game(placeHolder =  true)

    override fun prepare(team: Int, game: Game) {
        this.team = team
        this.game = game
        moveGenerator.setReferenceBoard(game.board)

    }

    override fun getColor(): Int {
        return this.team
    }



    override fun getName(): String {
        return "Random Mover"
    }

    override fun playMove(receiver: Holder<move>) {
        printBorder()
        val moves = moveGenerator.genAllLegalMoves(team).filter { it != 0 }
        val random = if (moves.size - 1 == 0 ) moves[0] else getRandom(moves)
        println("Trying: " + random.getString() )
        printBorder()
        receiver.hold(random)

    }

}