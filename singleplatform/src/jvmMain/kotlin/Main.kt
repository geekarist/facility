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
    MaterialTheme {
        props?.let {
            Button(onClick = {
                props.dispatch(App.Event.ButtonClicked)
            }) {
                Text(props.text)
            }
        } ?: Text("Loading...")
    }
}

fun main() {
    application {
        var appProps by rememberSaveable { mutableStateOf<App.Props?>(null) }
        Window(onCloseRequest = ::exitApplication) {
            App(appProps)
        }
        val coroutineScope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
            runtime(
                init = App::init,
                update = App::update,
                view = App::view,
                render = { props: App.Props ->
                    appProps = props
                    null
                },
                renderContext = coroutineScope.coroutineContext
            )
        }
    }
}
