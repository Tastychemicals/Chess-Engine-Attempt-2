package UI

import Base.Game

class VisualController {
    var game = Game(placeHolder =  true)
    var visualizer = Visualizer()
    var parser = Parser(game, visualizer)
    val parserLog = mutableListOf<String>()

    val lastMoveMask: Pair<Int,Int> get() = visualizer.lastMoveMask
    val pieceMask: List<Int> get() = visualizer.pieceMask
    val validMovesMask: Long get() = visualizer.validMovesMask
    val checkedKingMask: Int get() = visualizer.checkedKingMask
    val attackSquareMask: IntArray get() = visualizer.attackSquareMask
    val showSquareStrings: Boolean get() = visualizer.showSquareStrings
    val orientation: Int get() = visualizer.orientation

    // ------ Game ------
    fun switchToGame(game: Game) {
        this.game = game
        this.visualizer = Visualizer()
        this.parser = Parser(this.game, visualizer)
        this.visualizer.setBoard(game.board)
    }
    // ----------------
    // ---- Parser ----

    fun parseText(string: String) {
        parserLog.add(parser.parseCommand(string))
    }

    fun getParserOutput(): List<String> {
        return parserLog.toList()
    }
    // ----------------
    // ---- Visualizer ----
    fun updateVisuals() {
        visualizer.update()
    }
    fun clearVisuals() {
        visualizer.clearAllMasks()
    }
    fun viewFrom(color: Int) {
        visualizer.setNewOrientation(color)
    }

    fun addMoveSquareMasks(square: Int) {
        visualizer.addMoveSquareMasks(square)
    }
}