package com.example.bioplatform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 核心目录和快照任务接口的轻量集成验证；测试不会启动全量统计。 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class PlatformIntegrationTest {
    @Autowired MockMvc mvc;

    @Test
    void catalogMatchesCurrentDatabaseModel() throws Exception {
        mvc.perform(get("/api/data-types")).andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(4)));
        mvc.perform(get("/api/databases")).andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(261)));
        mvc.perform(get("/api/resource-recommendations")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.data[4].dataType.code").value("OTHER"));
    }

    @Test
    void searchDatabaseHitsAreMatchedThenGrouped() throws Exception {
        mvc.perform(post("/api/resource-recommendations/search-results")
                        .contentType(APPLICATION_JSON)
                        .content("[{\"id\":\"nucbank_nuc2\",\"name\":\"GenBase\"}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].dataType.code").value("DNA"));
    }

    @Test
    void snapshotStatusIsReachableWithoutRunningBulkBuild() throws Exception {
        mvc.perform(get("/api/research-snapshots/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.running").value(false));
        mvc.perform(post("/api/research-snapshots/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.running").value(false));
    }

    @Test
    void swaggerAndSystemStatusAreReachable() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.openapi", startsWith("3.")));
        mvc.perform(get("/api/system/status")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.database").value("H2"));
        mvc.perform(get("/api/collection/openlb/status")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.running").value(false));
    }
}
