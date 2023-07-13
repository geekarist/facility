package me.cpele.workitems.core.programs

import me.cpele.workitems.core.framework.Change
import me.cpele.workitems.core.framework.Platform
import me.cpele.workitems.core.framework.Prop
import me.cpele.workitems.core.framework.Slack

object Retrieved {

    data class Model(
        val accessToken: String,
        val id: String,
        val image: String,
        val imageBuffer: ImageBuffer? = null,
        val name: String,
        val realName: String,
        val email: String,
        val presence: String
    )

    sealed interface Event {
        data class FetchedUserImage(val bufferResult: Result<ByteArray>) : Event
        object SignOut : Event
    }

    data class Props(
        /** Account image. When absent, `null` */
        val image: Prop.Image?,
        val name: Prop.Text,
        val availability: Prop.Text,
        val token: Prop.Text,
        val email: Prop.Text,
        val signOut: Prop.Button,
    )

    interface Parent {
        suspend fun takeResult(result: Result<Unit>)
    }

    fun <Ctx> init(
        ctx: Ctx,
        accessToken: String,
        infoResult: Result<Slack.UserInfo>,
    ): Change<Model?, Event>
            where Ctx : Platform,
                  Ctx : Parent = run {
        infoResult.fold(
            onSuccess = { info ->
                val newModel = Model(
                    accessToken = accessToken,
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
            },
            onFailure = { throwable ->
                Change(null) {
                    ctx.takeResult(Result.failure(throwable))
                }
            }
        )
    }

    fun <Ctx> update(
        ctx: Ctx, model: Model?, event: Event
    ): Change<Model?, Event>
            where Ctx : Parent,
                  Ctx : Platform,
                  Ctx : Slack = run {
        checkNotNull(model) {
            "Can't update null retrieved account on event: $event"
        }
        when (event) {
            is Event.FetchedUserImage -> event.bufferResult.fold(
                onSuccess = { Change(model.copy(imageBuffer = ImageBuffer(it))) },
                onFailure = { throwable ->
                    Change(model) {
                        ctx.logi(throwable) { "Failed retrieving image ${model.image}" }
                    }
                }
            )

            Event.SignOut -> Change(model) {
                ctx.takeResult(Result.success(Unit))
            }
        }
    }
}
