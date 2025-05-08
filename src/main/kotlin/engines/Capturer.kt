package engines

import BoardUtils.Holder
import BoardUtils.getString
import BoardUtils.isCapture
import BoardUtils.isPromotion
import BoardUtils.move
import Base.Board
import Base.Game
import Base.MoveGenerator
import kotlin.random.Random

class Capturer : Engine() {
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
        TODO("Not yet implemented")
    }

    override fun makePlay(timeLimit: Long, board: Board, receiver: Holder<Int>) {
        TODO("Not yet implemented")
    }

    override fun getName(): String {
        return "Capturer"
    }

    override fun makePlayy(receiver: Holder<move>) {
        val moves = moveGenerator.genAllLegalMoves(team).filter { it != 0 }
        println("looking for captures...")
        val captures = moves.filter { it.isCapture() || it.isPromotion() }
        val move = if (captures.isEmpty()) {
            println("No captures or promotions found.")
            println("Playing random move.")
            getRandom(moves)
        } else {
            println("Favorable move found!")
            getRandom(captures)
        }
        println("Trying: " + move.getString() )
        receiver.hold(move)
    }



}