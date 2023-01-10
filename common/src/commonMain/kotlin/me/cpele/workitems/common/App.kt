package me.cpele.workitems.common

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun App() {
    val list = remember { listOf("toto", "titi", "tata") }
    LazyColumn {
        items(list) { itemData ->
            Text(itemData)
        }
    }
}
