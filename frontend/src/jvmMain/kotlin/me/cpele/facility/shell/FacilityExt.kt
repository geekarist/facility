package me.cpele.facility.shell

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import me.cpele.facility.core.programs.Facility
import me.cpele.facility.core.programs.SlackAccount

fun Facility.main(vararg args: String) {
    app(
        init = Facility::init,
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
