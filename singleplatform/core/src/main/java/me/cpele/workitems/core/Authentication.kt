package me.cpele.workitems.core

import kotlinx.coroutines.CoroutineScope
import oolong.Effect
import oolong.effect.none

/**
 * # Authentication program
 *
 * ## Model
 *
 * - List of providers
 * - Provider has description, optional account
 * - Account has info like login, tokens, avatar, etc.
 *
 * ## Update
 *
 * - Initially no provider
 * - Login requested for given provider ⇒ send request to provider, record pending auth status
 * - Provider authenticated account ⇒ store account info, record completed auth status
 * - Error requesting account ⇒ TODO
 * - Auth request refused ⇒ TODO
 * - Provider info requested ⇒ TODO (account info or provider info)
 * - Logout requested for provider ⇒ TODO
 *
 * ## View
 *
 * - List of icons, each of them a provider of work items
 * like Slack, Jira, etc.
 * - Provider without account displays as grayscale icon. On click shows *provider* info, *login* button.
 * - Provider with account displays as color icon. On click shows *account* info, *logout* button.
 */
object Authentication {
    fun init(): Pair<Model, Effect<Event>> = Model(emptyList()) to none()

    fun update(
        event: Event,
        model: Model
    ): Pair<Model, suspend CoroutineScope.((Event) -> Unit) -> Any?> = model to none()

    fun view(model: Model, function: (Event) -> Unit): Props = Props()

    class Event

    data class Model(val providers: List<Provider>) {
        class Provider {

        }
    }

    class Props(val text: String = "Yo")
}
