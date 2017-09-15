package no.uio.ifi.trackfind.backend.rest.controllers;

import no.uio.ifi.trackfind.TestTrackFindApplication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static no.uio.ifi.trackfind.TestTrackFindApplication.TEST_DATA_PROVIDER;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestTrackFindApplication.class)
public class TrackFindRESTTests {

    private static final String API_PREFIX = "/api/v1/";

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void getProvidersTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + "providers").param("published", "false"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]", is(TEST_DATA_PROVIDER)));
    }

    @Test
    public void getMetamodelTreeTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/metamodel-tree").param("advanced", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.key1", hasSize(2)))
                .andExpect(jsonPath("$.key1", containsInAnyOrder("value1", "value2")))
                .andExpect(jsonPath("$.key2", contains("value3")));
    }

    @Test
    public void getMetamodelFlatTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/metamodel-flat").param("advanced", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.key1", hasSize(2)))
                .andExpect(jsonPath("$.key1", containsInAnyOrder("value1", "value2")))
                .andExpect(jsonPath("$.key2", contains("value3")));
    }

    @Test
    public void getAttributesTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/attributes/").param("advanced", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$", contains("key1", "key2", "data_type")));
    }

    @Test
    public void getAttributesWithFilterTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/attributes").param("filter", "ey1").param("advanced", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$.[0]", is("key1")));
    }

    @Test
    public void getValuesSingleTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/key2/values"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$.[0]", is("value3")));
    }

    @Test
    public void getValuesMultipleTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/key1/values"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$", containsInAnyOrder("value1", "value2")));
    }

    @Test
    public void getValuesWithFilterTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/key1/values").param("filter", "lue2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$.[0]", is("value2")));
    }

    @Test
    public void searchTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/search").param("query", "key1: value1 OR key2: value3"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$.[0].key1", is("value1")))
                .andExpect(jsonPath("$.[1].key1", is("value2")))
                .andExpect(jsonPath("$.[1].key2", is("value3")));
    }

}
