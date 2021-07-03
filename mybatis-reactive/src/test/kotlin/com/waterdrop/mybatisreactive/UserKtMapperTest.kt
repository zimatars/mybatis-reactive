package com.waterdrop.mybatisreactive

import com.waterdrop.mybatisreactive.builder.xml.ReactiveXMLConfigBuilder
import com.waterdrop.mybatisreactive.entity.User
import com.waterdrop.mybatisreactive.executor.BaseReactiveExecutor
import com.waterdrop.mybatisreactive.mapper.UserKtMapper
import com.waterdrop.mybatisreactive.session.ReactiveSqlSessionFactory
import com.waterdrop.mybatisreactive.session.defaults.DefaultReactiveSqlSessionFactory
import kotlinx.coroutines.runBlocking
import org.apache.ibatis.logging.LogFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserKtMapperTest {
    private lateinit var reactiveSqlSessionFactory: ReactiveSqlSessionFactory

    @BeforeAll
    fun setUp() {
        val xmlConfigBuilder = ReactiveXMLConfigBuilder(this.javaClass.getResourceAsStream("/mybatis-config.xml"))
        val configuration = xmlConfigBuilder.parse()
        reactiveSqlSessionFactory = DefaultReactiveSqlSessionFactory(configuration)
    }

    private fun getUserKtMapper(): UserKtMapper {
        return reactiveSqlSessionFactory.openSession().getMapper(UserKtMapper::class.java)
    }


    @Test
    fun testGetById() = runBlocking{
        val user: User? = getUserKtMapper().getById(1)
        Assertions.assertTrue { user!=null && user.id==1L }
    }

    @Test
    fun testSelectList() = runBlocking{
        val users: List<User> = getUserKtMapper().selectList()
        Assertions.assertTrue { users.isNotEmpty() }
    }
}
