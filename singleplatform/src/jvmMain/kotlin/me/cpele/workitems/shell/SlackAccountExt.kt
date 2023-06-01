package me.cpele.workitems.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import me.cpele.workitems.core.Platform
import me.cpele.workitems.core.Prop
import me.cpele.workitems.core.Slack
import me.cpele.workitems.core.SlackAccount

fun SlackAccount.main(vararg args: String) { // TODO: SlackAccount.main(args)
    if (args.contains("mock")) {
        SlackAccount.makeApp(MockSlack, DesktopPlatform)
    } else {
        SlackAccount.makeApp(DefaultSlack(DesktopPlatform, NgrokIngress), DesktopPlatform)
    }
}

private fun SlackAccount.makeApp(slack: Slack, platform: Platform) {
    app(
        init = ::init,
        update = makeUpdate(slack, platform),
        view = ::view,
        ui = {
            Ui(it)
        }
    )
}

@Composable
private fun SlackAccount.Ui(props: SlackAccount.Props) {
    MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (props) {

                is SlackAccount.Props.SignedIn -> SignedIn(props)
                is SlackAccount.Props.SignedOut -> SignedOut(props)
                is SlackAccount.Props.SigningIn -> SigningIn(props)
            }
        }
    }
}

@Composable
private fun SignedIn(props: SlackAccount.Props.SignedIn) {
    var painter: Painter? by remember { mutableStateOf(null) }
    LaunchedEffect(props.image) {
        painter = props.image?.asBitmap()?.toAwtImage()?.toPainter()
    }
    painter?.let {
        Image(it, contentDescription = null)
    } ?: run {
        val pkg = SlackAccount::class.java.`package`.name
        val path = pkg.replace('.', '/')
        val placeholder = "$path/placeholder.svg"
        Image(painter = painterResource(placeholder), contentDescription = null)
    }
}

private fun Prop.Image.asBitmap(): ImageBitmap {
    val bitmap = buffer.inputStream().use { stream ->
        loadImageBitmap(stream)
    }
    return bitmap
}

@Composable
private fun SigningIn(props: SlackAccount.Props.SigningIn) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = props.title.text, style = MaterialTheme.typography.h4)
        props.statuses.forEach {
            Text(it.text)
        }
        CircularProgressIndicator()
        Button(props.cancel.onClick) {
            Text(text = props.cancel.text)
        }
    }
}

@Composable
private fun SignedOut(props: SlackAccount.Props.SignedOut) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = props.title.text, style = MaterialTheme.typography.h4)
        Text(props.desc.text)
        Button(
            onClick = props.button.onClick,
            enabled = props.button.isEnabled
        ) {
            Text(text = props.button.text)
        }
    }
}
