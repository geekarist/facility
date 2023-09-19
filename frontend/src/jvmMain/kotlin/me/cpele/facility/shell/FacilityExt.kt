package me.cpele.facility.shell

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import me.cpele.facility.core.framework.effects.AppRuntime
import me.cpele.facility.core.programs.Facility
import me.cpele.facility.core.programs.SlackAccount

fun Facility.main() {
    val appInit = {
        init(
            DefaultSlack(DesktopPlatform, NgrokIngress(DesktopPlatform)),
            DesktopPlatform,
            object : AppRuntime {
                override suspend fun exit() {}
            },
            DesktopPreferences,
            DesktopStore
        )
    }
    app(
        init = appInit,
        update = Facility::update,
        view = Facility::view,
        setOnQuitListener = {},
        ui = { props -> Ui(props) }
    )
}

@Composable
private fun Facility.Ui(props: Facility.Props) = run {
    Window(onCloseRequest = {}) {
        Text("Hello Facility")
        SlackAccount.Ui(props.slackAccount)
    }
}

/**
 * Workaround to run the program easily from the IDE, because [Facility.main] fails when launched that way.
 */
private fun main() = Facility.main()
