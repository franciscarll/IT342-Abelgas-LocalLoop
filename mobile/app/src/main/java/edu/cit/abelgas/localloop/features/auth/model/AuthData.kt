package edu.cit.abelgas.localloop.features.auth.model

import edu.cit.abelgas.localloop.features.profile.model.UserDto

data class AuthData(
    val user: UserDto,
    val accessToken: String
)