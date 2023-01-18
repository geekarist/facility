package me.cpele.workitems.shell

import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import me.cpele.workitems.core.App
import oolong.runtime

@Composable
fun Ui(props: App.Props?) {
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
            Ui(appProps)
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
