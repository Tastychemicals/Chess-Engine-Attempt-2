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
import BoardUtils.KNIGHT
import BoardUtils.PAWN
import BoardUtils.QUEEN
import BoardUtils.ROOK
import BoardUtils.end
import BoardUtils.getOppositeColor
import BoardUtils.getPromotion
import BoardUtils.isCastle
import BoardUtils.isCheck
import BoardUtils.isEnPassant
import BoardUtils.printBorder
import BoardUtils.start
import kotlin.random.Random

class MasterOfZeroDepth : Engine() {
    val mg = MoveGenerator(Board())
    var team = -1
    var board = Board()
    var game = Game(placeHolder =  true)
    override fun prepare(team: Int, game: Game) {
        this.team = team
        this.game = game
        mg.setReferenceBoard(game.board)
    }


    override fun getColor(): Int {
        return this.team
    }

    override fun getName(): String {
        return "Master Of Zero Depth"
    }


    fun bbcontains(square: Int, bb: Long): Boolean {
        return  (1L shl square) and bb != 0L
    }
    val equals = mutableListOf<move>()
    fun getBest(moveScores: Map<move, Int>): move {
        var bestScore = -1000
        var bestMove = -1000
        for (move in moveScores.keys) {
            if (moveScores[move]!! > bestScore) {
                equals.clear()
                equals.add(move)
                bestScore = moveScores[move]!!
                bestMove =move
            } else if (moveScores[move]!! == bestScore) {
                equals.add(move)
            }
        }
        return bestMove
    }
    fun valueOf(type: Int): Int {
        return when (type) {
            KNIGHT -> return 3
            ROOK -> return 5
            QUEEN -> return 9
            BISHOP -> return 3
            PAWN -> 1
            else -> 0
        }
    }

    override fun playMove(receiver: Holder<move>) {
        printBorder()
        val moves = mg.genAllLegalMoves(team).filter {  it != 0 }
        val enemyAttack = mg.enemyAttackSquares.clone()
        val enemyMoves = mg.genAllLegalMoves(getOppositeColor(team))
        val myAttack = mg.enemyAttackSquares.clone()

        val enemylesserDefense = moves.filter { myAttack[it.end()] > enemyAttack[it.end()] && game.board.fetchPiece(it.end()).isOccupied() }
        val meLesserDef = moves.filter { enemyAttack[it.start()] >= myAttack[it.start()]  }
        val best = mutableMapOf<move, Int>()

        moves.forEach { best[it] = 0 }

        moves.forEach {
            if (it.isCapture()) {
                if (it in enemylesserDefense) {
                    best[it] = best[it]?.plus(game.board.fetchPiece(it.end()).value())  as Int
                } else best[it] = best[it]?.plus(game.board.fetchPiece(it.end()).value() - game.board.fetchPiece(it.start()).value() + 2) as Int

            }
            if (it.isCheck()) {
                if (it in meLesserDef) {
                    best[it] = best[it]?.minus(game.board.fetchPiece(it.start()).value()) as Int
                }
                best[it] = best[it]?.plus(1) as Int
            }
            if (it.isPromotion()) {
                best[it] = best[it]?.plus(valueOf(it.getPromotion()) ) as Int
            }
            if (it.isEnPassant()) {
                best[it] = best[it]?.plus(1)  as Int
            }
            if (it.isCastle()) {
                best[it] = best[it]?.plus(2)  as Int
            }
            if (it in enemylesserDefense) {
                best[it] = best[it]?.plus(2)  as Int
            }
            if (!it.isCastle() && game.board.fetchPiece(it.start()).isKing()) {
                best[it] = best[it]?.minus(1)  as Int
            }
            if (it.isCheck() && it.isPromotion()) {
                best[it] = best[it]?.plus(3)  as Int
            }
            if (!game.board.fetchPiece(it.start()).hasMoved) {
                best[it] = best[it]?.plus(1)  as Int
            }
            if (it in meLesserDef) {
                best[it] = best[it]?.plus(2)  as Int
            }
            if (enemyAttack[it.end()] > myAttack[it.end()]) {
                best[it] = best[it]?.minus(1)  as Int
            }

        }







       // val meWinTrade = moves.filter {  game.board.fetchPiece(it.end()).value() >= game.board.fetchPiece(it.start()).value() }



       // val nonMovedPieceeEcludingking = moves.filter { !game.board.fetchPiece(it.start()).hasMoved && !game.board.fetchPiece(it.start()).isKing() }




//        val m = moves.filter {(( game.board.fetchPiece(it.end()).value() >= game.board.fetchPiece(it.start()).value()
//                && game.board.fetchPiece(it.end()).value() != 0 )
//                || ( moveGenerator.enemyAttackSquares[it.end()] == 0
//                && !(game.board.fetchPiece(it.end()).value() >= game.board.fetchPiece(it.start()).value()
//                && game.board.fetchPiece(it.end()).value() != 0 ))
//
//        ) }
        println("The Master is working...")
        printBorder()
        val g = best.toSortedMap().reversed().entries.toList()
        for (r in 0..best.size - 1) {
            val f = g[r]
            //if (r == 6) break
            println("${f.key.getString()} , ${f.value}   ")
        }
        printBorder()
        //println(m.size)
        //val t = m.filter { it.isPromotion() &&  it.getPromotion() == QUEEN }
        //if (t.isNotEmpty()) {receiver.hold(getRandom(t)); return}
        //println("The Master couldn't find a way to bludgeon you...")
        //val f = m.filter { (it.isCapture() || it.isEnPassant())}
        //if (f.isNotEmpty()) {receiver.hold(getRandom(f)); return}
        //println("The Master couldn't find his advantage here...")

        //val x = m.filter { (it.isCheck() || it.isCastle())}
        //if (x.isNotEmpty()) {receiver.hold(getRandom(x)); return}
        //println("hmmm...")

//        val third = moves.filter { it.isPromotion() &&  it.getPromotion() == QUEEN }
//        if (third.isNotEmpty()) {receiver.hold(getRandom(third)); return}
//        println("The Master couldn't find a way to bludgeon you...")
//        val first = moves.filter { it.isCheck() && (it.isCapture() || it.isEnPassant())}
//        if (first.isNotEmpty()) {receiver.hold(getRandom(first)); return}
//        println("The Master couldn't find his advantage...")


        //val second = moves.filter { it.isCheck() || it.isCapture() || it.isEnPassant()|| it.isCastle() }
        //if (second.isNotEmpty()) {receiver.hold(getRandom(second));return}
        //println("The Master is being lenient...")


        //val captures = moves.filter { true }
//        val move = if (captures.isEmpty()) {
//            println("Nothing to do.")
//            println("Playing random move.")
//            getRandom(moves)
//        } else {
//            println("Master Chose his move")
//            getRandom(captures)
//        }
        val move = getBest(best)
        val move1 = equals[Random.nextInt(0, equals.size)]
        println("The Master Chose His Move.")
        println("Trying: " + move1.getString() )
        printBorder()
        receiver.hold(move1)
    }



}