package com.waterdrop.mybatisreactive.mapper

import com.waterdrop.mybatisreactive.entity.User
import reactor.core.publisher.Flux

interface UserKtMapper {
    suspend fun getById(id: Long?): User?

    suspend fun selectList(): List<User>
}
