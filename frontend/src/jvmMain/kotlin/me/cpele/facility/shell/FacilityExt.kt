package me.cpele.facility.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Window
import me.cpele.facility.core.framework.effects.AppRuntime
import me.cpele.facility.core.programs.Facility
import me.cpele.facility.core.programs.Facility.Ctx
import me.cpele.facility.core.programs.SlackAccount
import kotlin.system.exitProcess

fun Facility.main(vararg args: String) {
    val slack = if (args.contains("mock")) {
        MockSlack
    } else {
        DefaultSlack(DesktopPlatform, NgrokIngress(DesktopPlatform))
    }
    app(
        init = ::init,
        update = makeUpdate(
            Ctx.of(
                DesktopPlatform,
                slack,
                AppRuntime.of { exitProcess(0) },
                DesktopPreferences,
                DesktopStore
            )
        ),
        view = ::view,
        setOnQuitListener = {},
        ui = { props -> Ui(props) }
    )
}

@Composable
private fun Facility.Ui(props: Facility.Props) = run {
    Window(onCloseRequest = props.onWindowClose) {
        Box(contentAlignment = Companion.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                Text(style = MaterialTheme.typography.h3, text = "Facility")
                Text(
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center,
                    text = """
                    Lorem ipsum dolor sit amet, consectetur adipiscing elit, 
                    sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. 
                    Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris 
                    nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in 
                    reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. 
                    Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia 
                    deserunt mollit anim id est laborum.
                    """.trimIndent()
                )
                Button(
                    onClick = props.openSlackAccount.onClick,
                    enabled = props.openSlackAccount.isEnabled
                ) {
                    Text(text = props.openSlackAccount.text)
                }
            }
        }
    }
    props.slackAccount?.let { subProps ->
        SlackAccount.Ui(subProps)
    }
}

/**
 * Workaround to run the program easily from the IDE, because [Facility.main] fails when launched directly.
 */
fun main(args: Array<String>) = Facility.main(*args)
