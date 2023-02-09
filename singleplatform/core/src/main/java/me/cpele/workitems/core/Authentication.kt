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
 * - Provider has description, optional account, interface
 * - Account has info like login, tokens, avatar, etc.
 * - Auth status: absent, pending, failure, complete
 * - Status message
 *
 * ## Update
 *
 * - Initially no provider
 * - Login requested for given provider ⇒ contact provider to initiate login, record pending auth status
 * - Provider authenticated account ⇒ record account info, record completed auth status
 * - Error requesting account ⇒ record error status message
 * - Auth request refused ⇒ record failure status message
 * - Provider info requested ⇒ expose appropriate info, either about provider or account
 * - Logout requested ⇒ contact provider to request logout, record deleted auth status
 *
 * ## View
 *
 * - List of icons, each of them a provider of work items like Slack, Jira, etc.
 * - Provider without account displays as grayscale icon. On click show *provider* info, *login* button.
 * - Provider with account displays as color icon. On click show *account* info, *logout* button.
 * - Optional error message modal dialog
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
