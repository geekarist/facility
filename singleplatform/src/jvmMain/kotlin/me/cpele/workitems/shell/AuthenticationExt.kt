@file:Suppress("UnusedReceiverParameter")

package me.cpele.workitems.shell

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import me.cpele.workitems.core.Accounts
import me.cpele.workitems.core.Platform
import me.cpele.workitems.core.Slack

@Composable
fun Accounts.Ui(modifier: Modifier = Modifier, props: Accounts.Props) {
    val buttonTextList by remember(props.buttons) {
        derivedStateOf { props.buttons.map { it.text } }
    }
    WithLargestTextWidth(
        buttonTextList,
        dependeeContent = { Button({}) { Text(it) } }
    ) { textWidthDp: Dp ->
        Column(
            modifier = modifier.padding(16.dp).wrapContentSize(unbounded = true)
        ) {
            props.buttons.forEach { button ->
                Button(onClick = button.onClick) {
                    Text(text = button.text, modifier = Modifier.width(textWidthDp), textAlign = TextAlign.Center)
                }
            }
        }
    }
    props.dialog?.let { dialog ->
        Dialog(onCloseRequest = { dialog.onClose() }) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dialog.texts.forEach { Text(it) }
                Button(onClick = dialog.button.onClick, enabled = dialog.isButtonEnabled) {
                    Text(dialog.button.text)
                }
            }
        }
    }
}

private enum class Slot {
    Dependent, Dependencies
}

/**
 * This composable determines the largest width of provided [texts]
 * as rendered in [dependeeContent] (just a [Text] by default)
 * and calls [dependentContent], giving that width to it.
 */
@Composable
fun WithLargestTextWidth(
    texts: List<String>,
    dependeeContent: @Composable (String) -> Unit = { Text(it) },
    dependentContent: @Composable (Dp) -> Unit
) {
    SubcomposeLayout { constraints ->
        val textWidthDpList = subcompose(Slot.Dependencies) {
            texts.forEach {
                dependeeContent(it)
            }
        }.map { it.measure(Constraints()).width.toDp() }
        val largestTextWidthDp = textWidthDpList.max()

        val dependentPlaceable = subcompose(Slot.Dependent) {
            dependentContent(largestTextWidthDp)
        }.first().measure(constraints)

        layout(dependentPlaceable.width, dependentPlaceable.height) {
            dependentPlaceable.place(0, 0)
        }
    }
}

fun Accounts.makeApp(platform: Platform, slack: Slack) = app(
    init = ::init,
    update = makeUpdate(slack, platform),
    view = ::view
) { Ui(props = it) }
