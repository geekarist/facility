package me.cpele.facility.core.programs

import me.cpele.facility.core.framework.Change
import me.cpele.facility.core.framework.Prop
import me.cpele.facility.core.framework.effects.*
import oolong.dispatch.contramap
import oolong.effect.map
import me.cpele.facility.core.programs.SlackAccount.Event as SlackAccountSubEvent

object Facility {
    data class Model(val slackAccount: SlackAccount.Model? = null)

    sealed interface Message {
        object Close : Message
        object OpenSlackAccount : Message

        data class SlackAccount(val subEvent: SlackAccountSubEvent) : Message
    }

    fun init(): Change<Model, Message> = Change(model = Model())

    fun makeUpdate(ctx: Ctx): (Message, Model) -> Change<Model, Message> = { message, model ->
        val subCtx = SlackAccount.Ctx(ctx, ctx, ctx, ctx)

        when (message) {
            Message.Close -> Change(model) {
                ctx.exit()
            }

            Message.OpenSlackAccount -> {
                val subChange = SlackAccount.init(subCtx)
                val newModel = model.copy(slackAccount = subChange.model)
                val newEffect = map(subChange.effect, Message::SlackAccount)
                Change(newModel, newEffect)
            }

            is Message.SlackAccount -> {
                if (message.subEvent == SlackAccount.Event.Intent.Quit) {
                    Change(model.copy(slackAccount = null))
                } else {
                    val subModel = model.slackAccount
                    checkNotNull(subModel) { "Missing sub model in model: $model" }
                    val subEvent = message.subEvent
                    val subChange = SlackAccount.update(subCtx, subEvent, subModel)
                    val newModel = model.copy(slackAccount = subChange.model)
                    val newEffect = map(subChange.effect, Message::SlackAccount)
                    Change(newModel, newEffect)
                }
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

    class Props(
        val onWindowClose: () -> Unit,
        val slackAccount: SlackAccount.Props?,
        val openSlackAccount: Prop.Button
    )

    fun view(model: Model, dispatch: (Message) -> Unit): Props {
        val slackAccountProps = model.slackAccount?.let { subModel ->
            SlackAccount.view(
                subModel,
                contramap(dispatch, Message::SlackAccount)
            )
        }
        return Props(
            openSlackAccount = Prop.Button("Slack account") {
                dispatch(Message.OpenSlackAccount)
            },
            slackAccount = slackAccountProps,
            onWindowClose = {
                slackAccountProps?.onWindowClose?.invoke()
                dispatch(Message.Close)
            }
        )
    }
}

