package me.cpele.workitems.core.programs

import me.cpele.workitems.core.framework.Change
import me.cpele.workitems.core.framework.Prop
import me.cpele.workitems.core.framework.effects.Platform
import me.cpele.workitems.core.framework.effects.Slack

object SlackRetrievedAccount {

    data class Model(
        val credentials: Slack.Credentials,
        val id: String,
        val image: String,
        val imageBuffer: ImageBuffer? = null,
        val name: String,
        val realName: String,
        val email: String,
        val presence: String
    ) {
        val accessToken: String = credentials.userToken
    }

    sealed interface Event {
        data class FetchedUserImage(val bufferResult: Result<ByteArray>) : Event
        object SignOut : Event
        object Refresh : Event
    }

    data class Props(
        /** Account image. When absent, `null` */
        val image: Prop.Image?,
        val name: Prop.Text,
        val availability: Prop.Text,
        val token: Prop.Text,
        val email: Prop.Text,
        val refresh: Prop.Button,
        val signOut: Prop.Button,
    )

    fun <Ctx : Platform> init(
        ctx: Ctx,
        credentials: Slack.Credentials,
        info: Slack.UserInfo,
    ): Change<Model, Event> = run {
        val newModel = Model(
            credentials = credentials,
            id = info.id,
            image = info.image,
            name = info.name,
            realName = info.realName,
            email = info.email,
            presence = info.presence
        )
        Change(newModel) { dispatch ->
            val imageUrl = newModel.image
            val bufferResult = ctx.fetch(imageUrl)
            dispatch(Event.FetchedUserImage(bufferResult))
        }
    }

    fun view(model: Model, dispatch: (Event) -> Unit): Props =
        Props(
            image = model.imageBuffer?.let { Prop.Image(it.array) },
            name = Prop.Text(model.realName),
            availability = Prop.Text("Presence: ${model.presence}"),
            token = Prop.Text("Access token: ${model.accessToken}"),
            email = Prop.Text("Email: ${model.email}"),
            refresh = Prop.Button("Refresh") { dispatch(Event.Refresh) },
            signOut = Prop.Button("Sign out") { dispatch(Event.SignOut) }
        )

    fun <Ctx> update(
        ctx: Ctx, model: Model, event: Event
    ): Change<Model, Event>
            where Ctx : Platform,
                  Ctx : Slack =
        when (event) {
            is Event.FetchedUserImage -> event.bufferResult.fold(
                onSuccess = { Change(model.copy(imageBuffer = ImageBuffer(it))) },
                onFailure = { throwable ->
                    Change(model) {
                        ctx.logi(throwable) { "Failed retrieving image ${model.image}" }
                    }
                }
            )

            Event.SignOut -> error("Sign-out event must be handled by caller")
            Event.Refresh -> error("Refresh event must be handled by caller")
        }
}
