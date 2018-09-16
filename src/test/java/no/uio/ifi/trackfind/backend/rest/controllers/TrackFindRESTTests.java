package no.uio.ifi.trackfind.backend.rest.controllers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import no.uio.ifi.trackfind.backend.dao.Dataset;
import no.uio.ifi.trackfind.backend.data.providers.DataProvider;
import no.uio.ifi.trackfind.backend.services.TrackFindService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
@RunWith(SpringRunner.class)
@WebMvcTest
public class TrackFindRESTTests {

    private static final String TEST_DATA_PROVIDER = "TEST";
    private static final String API_PREFIX = "/api/v1/";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataProvider dataProvider;

    @MockBean
    private TrackFindService trackFindService;

    private Dataset dataset;

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
                .andExpect(jsonPath("$.level1.level2_1", hasSize(2)))
                .andExpect(jsonPath("$.level1.level2_1", containsInAnyOrder("value1", "value2")))
                .andExpect(jsonPath("$.level1.level2_2", contains("value3")));
    }

    @Test
    public void getMetamodelFlatTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/metamodel-flat"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.level1>level2_1", hasSize(2)))
                .andExpect(jsonPath("$.level1>level2_1", containsInAnyOrder("value1", "value2")))
                .andExpect(jsonPath("$.level1>level2_2", contains("value3")));
    }

    @Test
    public void getAttributesWithFilterTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/attributes").param("filter", "vel2_2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$.[0]", is("level1>level2_2")));
    }

    @Test
    public void getValuesSingleTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/level1>level2_2/values"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$.[0]", is("value3")));
    }

    @Test
    public void getValuesMultipleTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/level1>level2_1/values"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$", containsInAnyOrder("value1", "value2")));
    }

    @Test
    public void getValuesWithFilterTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/level1>level2_1/values").param("filter", "lue2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$.[0]", is("value2")));
    }

    @Test
    public void searchTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/search").param("query", "someQuery"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$.[0].id", is(0)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void fetchTest() throws Exception {
        mockMvc.perform(get(API_PREFIX + TEST_DATA_PROVIDER + "/fetch").param("documentId", "0"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.id", is(0)));
    }

    @Before
    public void setUp() {
        when(trackFindService.getDataProviders()).thenReturn(Collections.singleton(dataProvider));
        when(trackFindService.getDataProvider(anyString())).thenReturn(dataProvider);
        when(dataProvider.getName()).thenReturn(TEST_DATA_PROVIDER);
        Multimap<String, String> metamodelFlat = HashMultimap.create();
        metamodelFlat.put("level1>level2_1", "value1");
        metamodelFlat.put("level1>level2_1", "value2");
        metamodelFlat.put("level1>level2_2", "value3");
        when(dataProvider.getMetamodelFlat()).thenReturn(metamodelFlat);
        Map<String, Object> metamodelTree = new HashMap<>();
        Map<String, Object> metamodelTreeInner = new HashMap<>();
        metamodelTreeInner.put("level2_1", Arrays.asList("value1", "value2"));
        metamodelTreeInner.put("level2_2", Collections.singleton("value3"));
        metamodelTree.put("level1", metamodelTreeInner);
        when(dataProvider.getMetamodelTree()).thenReturn(metamodelTree);
        dataset = new Dataset();
        dataset.setId(0L);
        when(dataProvider.search(anyString(), anyInt())).thenReturn(Collections.singleton(dataset));
        when(dataProvider.fetch(anyString(), Mockito.any())).thenReturn(dataset);
    }

}
