package no.uio.ifi.trackfind.backend.rest.controllers;

import com.google.gson.Gson;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.data.providers.TestDataProvider;
import org.apache.commons.collections4.MapUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Collection;
import java.util.Map;

import static no.uio.ifi.trackfind.backend.data.providers.TestDataProvider.TEST_DATA_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@RunWith(SpringRunner.class)
@WebMvcTest
public class TrackFindRESTTests {

    private static final String API_PREFIX = "/api/v1/";

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private Gson gson;

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
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/metamodel-tree"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.Advanced.key1", hasSize(2)))
                .andExpect(jsonPath("$.Advanced.key1", containsInAnyOrder("value1", "value2")))
                .andExpect(jsonPath("$.Advanced.key2", contains("value3")));
    }

    @Test
    public void getMetamodelFlatTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/metamodel-flat"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.Advanced>key1", hasSize(2)))
                .andExpect(jsonPath("$.Advanced>key1", containsInAnyOrder("value1", "value2")))
                .andExpect(jsonPath("$.Advanced>key2", contains("value3")));
    }

    @Test
    public void getAttributesWithFilterTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/attributes").param("filter", "ey1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$.[0]", is("Advanced>key1")));
    }

    @Test
    public void getValuesSingleTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/Advanced>key2/values"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$.[0]", is("value3")));
    }

    @Test
    public void getValuesMultipleTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/Advanced>key1/values"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$", containsInAnyOrder("value1", "value2")));
    }

    @Test
    public void getValuesWithFilterTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/Advanced>key1/values").param("filter", "lue2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$.[0]", is("value2")));
    }

    @Test
    public void searchTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/search").param("query", "Advanced>key2: value3"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.*", hasSize(1)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fetchTest() throws Exception {
        String searchResponse = mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/search").param("query", "Advanced>key2: value3"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andReturn().getResponse().getContentAsString();
        Map search = (Map) ((Collection) gson.fromJson(searchResponse, Map.class).values().iterator().next()).iterator().next();
        search = MapUtils.getMap(search, "Advanced");
        String id = search.remove("id").toString();
        String fetchResponse = mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/fetch").param("documentId", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andReturn().getResponse().getContentAsString();
        Map fetch = gson.fromJson(fetchResponse, Map.class);
        assertThat(search).isEqualTo(fetch);
    }

    @ComponentScan(basePackageClasses = no.uio.ifi.trackfind.backend.services.TrackFindService.class)
    static class TestConfiguration {

        @Bean
        public DataProvider testDataProvider() {
            return new TestDataProvider();
        }

    }

}
