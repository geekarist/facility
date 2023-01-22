package me.cpele.workitems.core

import kotlinx.coroutines.CoroutineScope
import oolong.Dispatch

interface UiProgram<ModelT, EventT, PropsT> {
    fun init(): ModelT

    fun update(
        model: ModelT,
        event: EventT
    ): Pair<ModelT, suspend CoroutineScope.(Dispatch<EventT>) -> Any?>

    fun view(model: ModelT, dispatch: Dispatch<EventT>): PropsT
}
