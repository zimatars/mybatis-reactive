package com.waterdrop.mybatisreactive.sample.mapper

import com.waterdrop.mybatisreactive.sample.entity.User
import reactor.core.publisher.Mono

interface UserKtMapper {
    suspend fun getById(id: Long): User?

    suspend fun selectList(): List<User>

    suspend fun insert(user: User): Boolean
}
