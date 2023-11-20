package me.cpele.facility.shell

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
        Box(contentAlignment = Companion.Center, modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.wrapContentSize().padding(16.dp)
            ) {
                Text(style = MaterialTheme.typography.h3, text = "Facility")
                Spacer(Modifier.height(16.dp))
                Text(
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center,
                    text = """
                        Welcome to Facility.
                        This is a launcher program for several smaller composable utility programs.
                    """.trimIndent()
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(props.mockRemote.label, style = MaterialTheme.typography.body2)
                    Checkbox(checked = props.mockRemote.checked, onCheckedChange = { props.mockRemote.onToggle() })
                }
                Spacer(Modifier.height(8.dp))
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
