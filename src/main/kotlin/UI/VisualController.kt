package UI

import Base.Game

class VisualController {
    var game = Game()
    var visualizer = Visualizer()
    var parser = Parser(game, visualizer)
    val parserLog = mutableListOf<String>()
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
}