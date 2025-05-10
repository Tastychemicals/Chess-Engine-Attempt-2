package engines

import BoardUtils.Holder
import BoardUtils.getString
import BoardUtils.isCapture
import BoardUtils.isPromotion
import BoardUtils.move
import Base.Board
import Base.Game
import Base.MoveGenerator
import BoardUtils.BISHOP
import BoardUtils.QUEEN
import BoardUtils.ROOK
import BoardUtils.end
import BoardUtils.getPromotion
import BoardUtils.isCastle
import BoardUtils.isCheck
import BoardUtils.isEnPassant
import BoardUtils.start
import kotlin.random.Random

class MasterOfZeroDepth : Engine() {
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
        return "Master Of Zero Depth"
    }

    override fun playMove() {
        TODO("Not yet implemented")
    }

    override fun makePlayy(receiver: Holder<move>) {
        val moves = game.generator.genAllLegalMoves(team).filter {  it != 0 }
        val m = moves.filter {(( game.board.fetchPiece(it.end()).value() >= game.board.fetchPiece(it.start()).value()
                && game.board.fetchPiece(it.end()).value() != 0 )
                || ( moveGenerator.enemyAttackSquares[it.end()] == 0
                && !(game.board.fetchPiece(it.end()).value() >= game.board.fetchPiece(it.start()).value()
                && game.board.fetchPiece(it.end()).value() != 0 ))

        ) }
        println("The Master is working...")
        println(m.size)
        val t = m.filter { it.isPromotion() &&  it.getPromotion() == QUEEN }
        if (t.isNotEmpty()) {receiver.hold(getRandom(t)); return}
        println("The Master couldn't find a way to bludgeon you...")
        val f = m.filter { (it.isCapture() || it.isEnPassant())}
        if (f.isNotEmpty()) {receiver.hold(getRandom(f)); return}
        println("The Master couldn't find his advantage here...")

        val x = m.filter { (it.isCheck() || it.isCastle())}
        if (x.isNotEmpty()) {receiver.hold(getRandom(x)); return}
        println("hmmm...")

        val third = moves.filter { it.isPromotion() &&  it.getPromotion() == QUEEN }
        if (third.isNotEmpty()) {receiver.hold(getRandom(third)); return}
        println("The Master couldn't find a way to bludgeon you...")
        val first = moves.filter { it.isCheck() && (it.isCapture() || it.isEnPassant())}
        if (first.isNotEmpty()) {receiver.hold(getRandom(first)); return}
        println("The Master couldn't find his advantage...")


        val second = moves.filter { it.isCheck() || it.isCapture() || it.isEnPassant()|| it.isCastle() }
        if (second.isNotEmpty()) {receiver.hold(getRandom(second));return}
        println("The Master is being lenient...")


        val captures = moves.filter { true }
        val move = if (captures.isEmpty()) {
            println("Nothing to do.")
            println("Playing random move.")
            getRandom(moves)
        } else {
            println("Master Chose his move")
            getRandom(captures)
        }
        println("Trying: " + move.getString() )
        receiver.hold(move)
    }



}