package com.waterdrop.mybatisreactive.mapper

import com.waterdrop.mybatisreactive.entity.User

interface UserKtMapper {
    suspend fun getById(id: Long): User?

    suspend fun selectList(): List<User>
}
