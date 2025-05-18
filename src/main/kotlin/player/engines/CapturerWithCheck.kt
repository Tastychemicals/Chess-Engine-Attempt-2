package engines

import BoardUtils.Holder
import BoardUtils.getString
import BoardUtils.isCapture
import BoardUtils.isPromotion
import BoardUtils.move
import Base.Board
import Base.Game
import Base.MoveGenerator
import BoardUtils.isCheck
import BoardUtils.printBorder
import kotlin.random.Random

class CapturerWithCheck : Engine() {
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
        return "CapturerWithCheck"
    }

    override fun playMove(receiver: Holder<move>) {
        printBorder()
        val moves = moveGenerator.genAllLegalMoves(team).filter { it != 0 }
        println("looking for captures...")
        val captures = moves.filter { it.isCheck() || it.isCapture() }
        val move = if (captures.isEmpty()) {
            println("No captures, promotions, or checks found.")
            println("Playing random move.")
            getRandom(moves)
        } else {
            println("Favorable move found!")
            getRandom(captures)
        }
        println("Trying: " + move.getString() )
        printBorder()
        receiver.hold(move)
    }



}