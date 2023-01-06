import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import me.cpele.workitems.common.App


fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
