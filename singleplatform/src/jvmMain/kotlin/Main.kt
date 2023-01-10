import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import oolong.runtime

@Composable
@Preview
fun App(props: App.Props?) {
    var text by remember { mutableStateOf("Hello, World!") }

    MaterialTheme {
        Button(onClick = {
            text = "Hello, Desktop!"
        }) {
            Text(text)
        }
    }
}

fun main() {
    application {
        var appProps by rememberSaveable { mutableStateOf<App.Props?>(null) }
        Window(onCloseRequest = ::exitApplication) {
            App(appProps)
        }

        LaunchedEffect(Unit) {
            runtime(
                init = App::init,
                update = App::update,
                view = App::view,
                render = { props: App.Props ->
                    appProps = props
                    null
                })
        }
    }
}
