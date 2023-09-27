package me.cpele.facility.shell

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import me.cpele.facility.core.framework.effects.AppRuntime
import me.cpele.facility.core.programs.Facility
import me.cpele.facility.core.programs.Facility.Ctx
import me.cpele.facility.core.programs.SlackAccount
import kotlin.system.exitProcess

fun Facility.main() {
    val desktopInit = {
        val ctx = Ctx.of(
            DesktopPlatform,
            DefaultSlack(DesktopPlatform, NgrokIngress(DesktopPlatform)),
            object : AppRuntime {
                override suspend fun exit() {}
            },
            DesktopPreferences,
            DesktopStore
        )
        init()
    }
    app(
        init = desktopInit,
        update = makeUpdate(
            Ctx.of(
                DesktopPlatform,
                DefaultSlack(DesktopPlatform, NgrokIngress(DesktopPlatform)),
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
        Text("Hello Facility")
        props.slackAccount?.let { subProps ->
            SlackAccount.Ui(subProps)
        }
    }
}

/**
 * Workaround to run the program easily from the IDE, because [Facility.main] fails when launched directly.
 */
private fun main() = Facility.main()
