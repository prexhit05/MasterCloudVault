package com.masterCloudVault.MasterCloudVault.service;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Service
public class K8sDeploymentService {
    private static final Logger log = LoggerFactory.getLogger(K8sDeploymentService.class);

    @Autowired
    private ResourceLoader resourceLoader;

    private final KubernetesClient kubernetesClient = new DefaultKubernetesClient();

    // Inject the MySQL Host here, as it's needed for the DB_URL placeholder
    @Value("${mysql.provisioning.host:localhost}")
    private String mysqlHost;

    // Inject the Tenant Application image name
    @Value("${tenant.app.image:cloudvault/tenant-service:poc}")
    private String tenantAppImage;

    public K8sDeploymentService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    // --- Public method that calls the helper ---
    public void deployTenant(String tenantId, String dbUser, String dbPassword) throws IOException {
        log.info("Starting K8s deployment for tenantId={}, dbUser={}", tenantId, dbUser);

        // 1. Generate Deployment YAML
        log.debug("Loading deployment template for tenantId={}", tenantId);
        String deploymentYaml = loadAndReplaceTemplate("k8s-templates/deployment-template.yaml",
                tenantId, dbUser, dbPassword);
        log.debug("Generated deployment YAML for tenantId={} ({} chars)", tenantId, deploymentYaml.length());

        // 2. Generate Service YAML
        log.debug("Loading service template for tenantId={}", tenantId);
        String serviceYaml = loadAndReplaceTemplate("k8s-templates/service-template.yaml",
                tenantId, dbUser, dbPassword);
        log.debug("Generated service YAML for tenantId={} ({} chars)", tenantId, serviceYaml.length());

        // Apply the generated YAMLs to the Kubernetes cluster
        log.info("Applying Kubernetes Deployment and Service for tenantId={}", tenantId);


        try (InputStream deploymentStream = new ByteArrayInputStream(deploymentYaml.getBytes(StandardCharsets.UTF_8));
             InputStream serviceStream = new ByteArrayInputStream(serviceYaml.getBytes(StandardCharsets.UTF_8))) {

            String namespace = kubernetesClient.getNamespace();
            if (namespace == null || namespace.isEmpty()) {
                namespace = "default";
            }
            log.debug("Using Kubernetes namespace='{}' for tenantId={}", namespace, tenantId);

            kubernetesClient.load(deploymentStream).inNamespace(namespace).createOrReplace();
            log.info("Applied Deployment for tenantId={} in namespace={}", tenantId, namespace);

            kubernetesClient.load(serviceStream).inNamespace(namespace).createOrReplace();
            log.info("Applied Service for tenantId={} in namespace={}", tenantId, namespace);
        } catch (KubernetesClientException e) {
            log.error("Failed to apply Kubernetes resources for tenantId={}: {}", tenantId, e.getMessage(), e);
            throw e;
        }

        log.info("Completed Kubernetes deployment for tenantId={}", tenantId);
    }

    /**
     * Private helper method to load a YAML template and replace placeholders.
     */
    private String loadAndReplaceTemplate(String filename,
                                          String tenantId,
                                          String dbUser, String dbPassword) throws IOException {

        log.debug("Loading K8s template '{}' from classpath for tenantId={}", filename, tenantId);
        Resource resource = resourceLoader.getResource("classpath:" + filename);

        // Read the entire file content into a String
        String templateContent;
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            templateContent = FileCopyUtils.copyToString(reader);
            log.debug("Loaded template '{}' ({} chars)", filename, templateContent.length());
        } catch (IOException e) {
            log.error("Could not read K8s template file '{}' for tenantId={}", filename, tenantId, e);
            throw e;
        }

        // --- Substitution Logic ---

        // Calculate the full DB URL for the tenant
        String dbName = "db_" + tenantId;
        String fullDbUrl = "jdbc:mysql://" + mysqlHost + ":3306/" + dbName +
                "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";

        log.debug("Using DB URL='{}' for tenantId={}", fullDbUrl, tenantId);

        // Perform the string replacement
        String resolved = templateContent
                .replace("${TENANT_ID}", tenantId)
                .replace("${DB_URL}", fullDbUrl)
                .replace("${DB_USERNAME}", dbUser)
                .replace("${DB_PASSWORD}", dbPassword)
                .replace("${TENANT_APP_IMAGE}", tenantAppImage);

        log.debug("Finished placeholder substitution for template '{}' for tenantId={}", filename, tenantId);
        return resolved;
    }
}
