package me.cpele.facility.core.programs

import kotlinx.serialization.Serializable
import me.cpele.facility.core.framework.Change
import me.cpele.facility.core.framework.Prop
import me.cpele.facility.core.framework.effects.Platform
import me.cpele.facility.core.framework.effects.Slack

object SlackRetrievedAccount {

    @Serializable
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
        override fun toString(): String {
            return "Model(credentials=$credentials, id='$id', image='$image', imageBuffer=${
                imageBuffer.toString().take(96)
            }, name='$name', realName='$realName', email='$email', presence='$presence', accessToken='$accessToken')"
        }
    }

    sealed interface Event {
        data class FetchedUserImage(val bufferResult: Result<ByteArray>) : Event {
            override fun toString(): String {
                val bufStr = bufferResult.toString().take(96)
                return "FetchedUserImage(bufferResult=$bufStr)"
            }
        }

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
                  Ctx : Slack = run {
        check(event is Event.FetchedUserImage) {
            "Event must be handled by caller: $event"
        }
        event.bufferResult.fold(
            onSuccess = { Change(model.copy(imageBuffer = ImageBuffer(it))) },
            onFailure = { throwable ->
                Change(model) {
                    ctx.logi(throwable) { "Failed retrieving image ${model.image}" }
                }
            }
        )
    }
}
