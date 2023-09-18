package me.cpele.facility.shell

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import me.cpele.facility.core.framework.Prop
import me.cpele.facility.core.framework.effects.*
import me.cpele.facility.core.programs.SlackAccount
import me.cpele.facility.core.programs.SlackRetrievedAccount
import java.awt.Dimension
import kotlin.math.roundToInt

fun SlackAccount.main(vararg args: String) = run {
    if (args.contains("mock")) {
        SlackAccount.makeApp(MockSlack, DesktopPlatform, DesktopPreferences, DesktopStore)
    } else {
        SlackAccount.makeApp(
            DefaultSlack(DesktopPlatform, NgrokIngress(DesktopPlatform)),
            DesktopPlatform,
            DesktopPreferences,
            DesktopStore
        )
    }
}

private fun SlackAccount.makeApp(slack: Slack, platform: Platform, preferences: Preferences, store: Store) {
    var onQuitListener = {}
    val runtime = object : AppRuntime {
        override suspend fun exit() = onQuitListener()
    }
    val ctx = SlackAccount.Ctx(slack, platform, runtime, preferences, store)
    app(
        init = { init(ctx) },
        update = { event, model ->
            update(ctx, event, model)
        },
        view = ::view,
        setOnQuitListener = { onQuitListener = it },
        ui = { props ->
            Ui(props)
        }
    )
}

@Composable
private fun SlackAccount.Ui(props: SlackAccount.Props) = run {
    val windowIconPath = remember {
        val path = SlackAccount::class.java.pkgResPath()
        "$path/app-icon.png"
    }
    Window(
        title = props.windowTitle.text,
        onCloseRequest = props.onWindowClose,
        icon = painterResource(windowIconPath)
    ) {
        with(LocalDensity.current) {
            val minWidth = 600.dp.toPx().roundToInt()
            val minHeight = 700.dp.toPx().roundToInt()
            window.minimumSize = Dimension(minWidth, minHeight)
        }
        MaterialTheme {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (props) {
                    is SlackAccount.Props.SignedOut -> SignedOut(props)
                    is SlackAccount.Props.SigningIn -> SigningIn(props)
                    is SlackAccount.Props.Retrieved -> SignedIn(props.subProps)
                }
            }
        }
    }
}

private fun Class<SlackAccount>.pkgResPath(): String {
    val pkg = `package`.name
    val path = pkg.replace('.', '/')
    return path
}

@Composable
private fun SignedIn(props: SlackRetrievedAccount.Props) {
    val scrollState = rememberScrollState()
    ProvideTextStyle(TextStyle(textAlign = TextAlign.Center)) {
        Column(
            Modifier.verticalScroll(scrollState).padding(horizontal = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            var painter: Painter? by remember { mutableStateOf(null) }
            val density = LocalDensity.current
            LaunchedEffect(props.image, density) {
                painter = painter(props.image) ?: placeholderPainter(density)
            }
            painter?.let { actualPainter ->
                Image(painter = actualPainter, contentDescription = null, modifier = Modifier.width(256.dp))
                Spacer(Modifier.height(32.dp))
            }
            Text(text = props.name.text, style = LocalTextStyle.current.merge(MaterialTheme.typography.h4))
            Spacer(Modifier.height(16.dp))
            Text(props.availability.text)
            Spacer(Modifier.height(16.dp))
            Text(text = props.token.text, style = MaterialTheme.typography.body2.merge(LocalTextStyle.current))
            Spacer(Modifier.height(16.dp))
            Text(text = props.email.text, style = MaterialTheme.typography.body2.merge(LocalTextStyle.current))
            Spacer(Modifier.height(32.dp))
            Row {
                Button(props.refresh.onClick) {
                    Text(props.refresh.text)
                }
                Spacer(Modifier.width(16.dp))
                Button(props.signOut.onClick) {
                    Text(props.signOut.text)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun placeholderPainter(density: Density): Painter {
    val path = SlackAccount::class.java.pkgResPath()
    val placeholder = "$path/placeholder.svg"
    return useResource(placeholder) { inputStream ->
        loadSvgPainter(inputStream, density)
    }
}

private fun painter(image: Prop.Image?) =
    image?.loadBitmap()?.toAwtImage()?.toPainter()

private fun Prop.Image.loadBitmap(): ImageBitmap {
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
        props.statuses.forEach { status ->
            Text(status.text, textAlign = TextAlign.Center)
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
