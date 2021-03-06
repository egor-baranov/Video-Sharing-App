package com.example.tiktok_analog.data.register

import com.example.tiktok_analog.data.Result
import com.example.tiktok_analog.data.model.User
import java.io.IOException
import java.util.*

/**
 * Class that handles authentication w/ register credentials and retrieves profile information.
 */
class RegisterDataSource {

    fun register(username: String, password: String): Result<User> {
        return try {
            // TODO: handle registeredUser registration
            val fakeUser = User(UUID.randomUUID().toString(), "Jane Doe")
            Result.Success(fakeUser)
        } catch (e: Throwable) {
            Result.Error(IOException("Error logging in", e))
        }
    }
}