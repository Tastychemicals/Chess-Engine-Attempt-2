import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage

class MenuHandler {

    @FXML
    private lateinit var stage: Stage
    private lateinit var scene: Scene
    private lateinit var root: Parent


    @FXML
    fun startVsAI(e: ActionEvent) {

        load(e)

    }

    @FXML
    fun aIVsAI(e: ActionEvent) {

        load(e)
    }

    @FXML
    fun playerVsPlayer(e: ActionEvent) {

        load(e)
    }

    fun load(e: ActionEvent) {
        root = FXMLLoader.load(javaClass.getResource("Game2.fxml"))
        stage = ((e.source as Node).scene.window) as Stage
        scene = Scene(root)
        stage.scene = scene
        stage.show()
    }
}