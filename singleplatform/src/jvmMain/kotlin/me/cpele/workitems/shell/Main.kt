package me.cpele.workitems.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import me.cpele.workitems.core.App
import oolong.runtime

@Composable
fun Ui(props: App.Props?) {
    MaterialTheme {
        Box(Modifier.fillMaxSize().padding(32.dp)) {
            props?.let {
                Text(props.text)
            } ?: Text("Loading...")
        }
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
