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

    @Test
    fun testGetMetamodel() {
        given().`when`().get("/metamodel/$EXAMPLE/$EXAMPLE").then().statusCode(200).assertThat()
            .body(
                matchesJsonSchemaInClasspath("fairtracks.schema.json")
            )
    }

    @Test
    fun testGetCategories() {
        given().`when`().get("/categories/$EXAMPLE/$EXAMPLE").then().statusCode(200).assertThat()
            .body("", hasItems("tracks", "experiments", "samples", "studies", "non_standard_samples"))
    }

    @Test
    fun testGetAttributes() {
        given().`when`().get("/attributes/$EXAMPLE/$EXAMPLE/samples").then().statusCode(200).assertThat()
            .body(
                "",
                hasItems(
                    "local_id",
                    "global_id",
                    "biomaterial_type",
                    "sample_type"
                )
            )
    }

    @Test
    fun testGetSubAttributes() {
        given().`when`().get("/attributes/$EXAMPLE/$EXAMPLE/samples?path=sample_type").then()
            .statusCode(200)
            .assertThat()
            .body(
                "",
                hasItems(
                    "term_value",
                    "term_iri"
                )
            )
    }

    @Test
    fun testGetValues() {
        given().`when`().get("/values/$EXAMPLE/$EXAMPLE/samples?path=sample_type->term_value").then()
            .statusCode(200)
            .assertThat()
            .body(
                "",
                hasItems(
                    "H1-hESC"
                )
            )
    }

    @Test
    fun testGetValuesWithFilter() {
        given().`when`().get("/values/$EXAMPLE/$EXAMPLE/tracks?path=file_format->term_value&filter=ENCODE").then()
            .statusCode(200)
            .assertThat()
            .body(
                "",
                hasItems(
                    "ENCODE narrow peak format"
                )
            )
    }

}
