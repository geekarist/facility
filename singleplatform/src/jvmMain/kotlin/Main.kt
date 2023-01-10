import androidx.compose.material.MaterialTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

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
        val appProps by rememberSaveable { mutableStateOf<App.Props?>(null) }
        Window(onCloseRequest = ::exitApplication) {
            App(appProps)
        }

        LaunchedEffect(Unit) {
            // TODO: launch Oolong runtime
        }
    }
}

object App {
    data class Props(val text: String)
}
