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
import me.cpele.workitems.core.Hello
import oolong.runtime

@Deprecated("Was just to discover Oolong")
object HelloApp {
    @Composable
    fun Ui(props: Hello.Props?) {
        MaterialTheme {
            Box(Modifier.fillMaxSize().padding(32.dp)) {
                props?.let {
                    Text(props.text)
                } ?: Text("Loading...")
            }
        }
    }

    fun run() = application {
        var appProps by rememberSaveable { mutableStateOf<Hello.Props?>(null) }
        Window(onCloseRequest = ::exitApplication) {
            Ui(appProps)
        }
        val coroutineScope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
            runtime(
                init = Hello::init,
                update = Hello::update,
                view = Hello::view,
                render = { props: Hello.Props ->
                    appProps = props
                    null
                },
                renderContext = coroutineScope.coroutineContext
            )
        }
    }
}

fun main() = HelloApp.run()
