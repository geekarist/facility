package me.cpele.facility.core.programs

import me.cpele.facility.core.framework.Change
import me.cpele.facility.core.framework.effects.*
import oolong.dispatch.contramap
import oolong.effect.map
import oolong.effect.none

object Facility {
    data class Model(val slackAccount: SlackAccount.Model)

    data class Event(val event: SlackAccount.Event)

    fun init(
        slack: Slack,
        platform: Platform,
        runtime: AppRuntime,
        preferences: Preferences,
        store: Store
    ): Change<Model, Event> = run {
        val slackAccountInit = SlackAccount.init(
            SlackAccount.Ctx(
                slack, platform, runtime, preferences, store
            )
        )
        Change(
            Model(slackAccountInit.model),
            map(slackAccountInit.effect, ::Event)
        )
    }

    fun update(event: Event, model: Model): Change<Model, Event> = Change(model, none())

    class Props(val slackAccount: SlackAccount.Props)

    fun view(model: Model, dispatch: (Event) -> Unit): Props =
        Props(
            SlackAccount.view(
                model.slackAccount,
                contramap(dispatch, ::Event)
            )
        )
}