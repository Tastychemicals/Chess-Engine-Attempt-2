package UI

import Base.Game

class VisualController {
    var game = Game()
    var visualizer = Visualizer(game)
    var parser = Parser(game, visualizer)
    val parserLog = mutableListOf<String>()

    fun switchToGame(game: Game) {
        this.game = game
        this.visualizer = Visualizer(this.game)
        this.parser = Parser(this.game, visualizer)
    }

    fun parseText(string: String) {
        parserLog.add(parser.parseCommand(string))
    }

    fun getParserOutput(): List<String> {
        return parserLog.toList()
    }
    fun updateVisuals() {
        visualizer.updateCustomMask()
    }


}