@file:Suppress("UnusedReceiverParameter")

package me.cpele.workitems.shell

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.cpele.workitems.core.SignIn

@Composable
fun SignIn.Ui(modifier: Modifier = Modifier, props: SignIn.Props) {
    Text(modifier = modifier, text = props.text)
}