package me.cpele.workitems.core.framework

/**
 * Apply [block] to the encapsulated [InputT] value of [this] to obtain a [Result] of [OutputT].
 *
 * "Decapsulate" that latter [Result], catching any [Throwable].
 *
 * Re-encapsulate the resulting [OutputT] or the caught [Throwable] as a possible failure in addition to the original failure.
 */
inline fun <InputT, OutputT> Result<InputT>.flatMapCatching(block: (InputT) -> Result<OutputT>): Result<OutputT> =
    mapCatching { input ->
        block(input).getOrThrow()
    }