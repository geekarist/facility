package me.cpele.facility.core.programs

import me.cpele.facility.core.framework.Change
import me.cpele.facility.core.framework.effects.*
import oolong.dispatch.contramap
import oolong.effect.map
import me.cpele.facility.core.programs.SlackAccount.Event as SlackAccountSubEvent

object Facility {
    data class Model(val slackAccount: SlackAccount.Model)

    sealed interface Event {
        object CloseRequest : Event

        data class SlackAccount(val subEvent: SlackAccountSubEvent) : Event
    }

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
            map(slackAccountInit.effect, Event::SlackAccount)
        )
    }

    fun makeUpdate(ctx: Ctx): (Event, Model) -> Change<Model, Event> = { event, model ->
        when (event) {
            Event.CloseRequest -> Change(model) {
                ctx.exit()
            }

            is Event.SlackAccount -> {
                val subModel = model.slackAccount
                val subEvent = event.subEvent
                val subCtx = SlackAccount.Ctx(ctx, ctx, ctx, ctx, ctx)
                val subChange = SlackAccount.update(subCtx, subEvent, subModel)
                val newModel = model.copy(slackAccount = subChange.model)
                val newEffect = map<SlackAccount.Event, Event>(subChange.effect) { newSubEvent: SlackAccount.Event ->
                    Event.SlackAccount(newSubEvent)
                }
                Change(newModel, newEffect)
            }
        }
    }

    interface Ctx : Platform, Slack, AppRuntime, Preferences, Store {
        companion object {
            fun of(
                platform: Platform,
                slack: Slack,
                appRuntime: AppRuntime,
                prefs: Preferences,
                store: Store
            ): Ctx = object : Ctx,
                Platform by platform,
                Slack by slack,
                AppRuntime by appRuntime,
                Preferences by prefs,
                Store by store {}
        }
    }

    class Props(val onWindowClose: () -> Unit, val slackAccount: SlackAccount.Props)

    fun view(model: Model, dispatch: (Event) -> Unit): Props {
        val slackAccountProps = SlackAccount.view(
            model.slackAccount,
            contramap(dispatch, Event::SlackAccount)
        )
        return Props(
            slackAccount = slackAccountProps,
            onWindowClose = {
                slackAccountProps.onWindowClose()
                dispatch(Event.CloseRequest)
            }
        )
    }
}

