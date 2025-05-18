package player.HumanPlayers
import BoardUtils.Holder
import BoardUtils.move
import player.Player

class VisualPlayer : Human, Player {
    var team = -1
    override fun getName(): String {
        return "GUI Human"
    }
    override fun getColor(): Int {
        return this.team
    }

    override fun setColor(color: Int) {
        this.team = color
    }

}