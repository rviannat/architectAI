package com.architectai.backend;

import com.architectai.backend.model.Analysis;
import com.architectai.backend.model.Project;
import com.architectai.backend.service.AnalysisService;
import com.architectai.backend.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class Layer1ApiSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private AnalysisService analysisService;

    @Test
    void apiDocsShouldBeAvailable() throws Exception {
        mockMvc.perform(get("/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openapi").exists());
    }

    @Test
    void createProjectShouldValidateInput() throws Exception {
        mockMvc.perform(post("/api/v1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"repoUrl\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists())
            .andExpect(jsonPath("$.path").value("/api/v1/projects"));
    }

    @Test
    void createProjectShouldReturnStandardEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"repoUrl\":\"https://github.com/example/repo.git\",\"defaultBranch\":\"main\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.message").value("Project created"))
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.repoUrl").value("https://github.com/example/repo.git"));
    }

    @Test
    void listAgentsShouldExposeDomains() throws Exception {
        mockMvc.perform(get("/api/v1/agents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.count").value(greaterThan(0)))
            .andExpect(jsonPath("$.data.technicalCount").value(greaterThan(0)))
            .andExpect(jsonPath("$.data.agents[0].domain").exists());
    }

    @Test
    void createAnalysisShouldReturnStandardEnvelope() throws Exception {
        Project project = projectService.createProject("https://github.com/example/repo.git", "main");

        mockMvc.perform(post("/api/v1/projects/" + project.getId() + "/analyses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"CODE_REVIEW\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.message").value("Analysis created"))
            .andExpect(jsonPath("$.data.id").exists())
            .andExpect(jsonPath("$.data.type").value("CODE_REVIEW"));
    }

    @Test
    void reportEndpointShouldReturnAnalysisDetails() throws Exception {
        Project project = projectService.createProject("https://github.com/example/repo.git", "main");
        Analysis analysis = analysisService.createAnalysis(project.getId(), "CODE_REVIEW");
        analysisService.completeAnalysis(
            analysis.getId(),
            "/workspace/repo",
            7,
            "/tmp/report.pdf",
            "/tmp/commercial-report.pdf",
            "/tmp/manifest.md",
            120L
        );

        mockMvc.perform(get("/api/v1/analyses/" + analysis.getId() + "/reports"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.analysisId").value(analysis.getId()))
            .andExpect(jsonPath("$.data.technicalReport").value("/tmp/report.pdf"));
    }

    @Test
    void webhookShouldRejectInvalidSignature() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Hub-Signature-256", "sha256=invalid")
                .content("{\"repository\":{\"clone_url\":\"https://github.com/example/repo.git\"},\"ref\":\"refs/heads/main\"}"))
            .andExpect(status().isBadRequest());
    }
}

