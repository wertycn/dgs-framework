package com.netflix.graphql.dgs.autoconfig

import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.autoconfig.testcomponents.CustomContextBuilderConfig
import com.netflix.graphql.dgs.autoconfig.testcomponents.DataLoaderConfig
import com.netflix.graphql.dgs.autoconfig.testcomponents.HelloDatFetcherConfig
import com.netflix.graphql.dgs.exceptions.NoSchemaFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.core.io.ClassPathResource

class DgsAutoConfigurationTest {
    private val context = WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(DgsAutoConfiguration::class.java))!!

    @Test
    fun noSchemaException() {
        context.withClassLoader(FilteredClassLoader(ClassPathResource("schema/"))).run { ctx ->
            assertThat(ctx).failure.hasRootCauseInstanceOf(NoSchemaFoundException::class.java)
        }
    }

    @Test
    fun setsUpQueryExecutorWithDataFetcher() {
        context.withUserConfiguration(HelloDatFetcherConfig::class.java).run { ctx ->
            assertThat(ctx).getBean(DgsQueryExecutor::class.java).extracting {
                val executeQuery = it.executeAndExtractJsonPath<String>("query {hello}", "data.hello")
                assertThat(executeQuery).isEqualTo("Hello!")
            }
        }
    }

    @Test
    fun dataLoaderGetsRegistered() {
        context.withUserConfiguration(DataLoaderConfig::class.java).run { ctx ->
            assertThat(ctx).getBean(DgsQueryExecutor::class.java).extracting {
                val json = it.executeAndExtractJsonPath<List<String>>("{names}", "data.names")
                assertThat(json).isEqualTo(listOf("A", "B", "C"))
            }
        }
    }

    @Test
    fun mappedDataLoaderGetsRegistered() {
        context.withUserConfiguration(DataLoaderConfig::class.java).run { ctx ->
            assertThat(ctx).getBean(DgsQueryExecutor::class.java).extracting {
                val json = it.executeAndExtractJsonPath<List<String>>("{namesFromMapped}", "data.namesFromMapped")
                assertThat(json).isEqualTo(listOf("A", "B", "C"))
            }
        }
    }

    @Test
    fun customContext() {
        context.withUserConfiguration(CustomContextBuilderConfig::class.java).run { ctx ->
            assertThat(ctx).getBean(DgsQueryExecutor::class.java).extracting {
                val json = it.executeAndExtractJsonPath<Any>("{hello}", "data.hello")
                assertThat(json).isEqualTo("Hello custom context")
            }
        }
    }
}