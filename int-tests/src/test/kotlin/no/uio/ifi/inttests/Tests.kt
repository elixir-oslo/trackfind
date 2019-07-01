package no.uio.ifi.inttests

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath
import org.hamcrest.core.IsCollectionContaining.hasItems
import kotlin.test.BeforeTest
import kotlin.test.Test

const val EXAMPLE = "Example"

class Tests {

    @BeforeTest
    fun setup() {
        RestAssured.baseURI = "http://localhost"
        RestAssured.basePath = "/api/v1"
        RestAssured.port = 80
    }

    @Test
    fun testGetRepositories() {
        given().`when`().get("/repositories").then().statusCode(200).assertThat()
            .body("", hasItems(EXAMPLE))
    }

    @Test
    fun testGetHubs() {
        given().`when`().get("/hubs/$EXAMPLE").then().statusCode(200).assertThat()
            .body("", hasItems(EXAMPLE))
    }

//    @Test
//    fun testMetamodel() {
//        given().`when`().get("/metamodel/$EXAMPLE/$EXAMPLE").then().statusCode(200).assertThat()
//            .body(
//                matchesJsonSchemaInClasspath("fairtracks.schema.json")
//            )
//    }

}
