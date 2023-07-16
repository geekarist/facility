package me.cpele.workitems.core.programs

import me.cpele.workitems.core.framework.Prop

interface RetrievedProps {
    /** Account image. When absent, `null` */
    val image: Prop.Image?
    val name: Prop.Text
    val availability: Prop.Text
    val token: Prop.Text
    val email: Prop.Text
    val signOut: Prop.Button
}