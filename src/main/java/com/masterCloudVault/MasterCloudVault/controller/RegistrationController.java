package com.masterCloudVault.MasterCloudVault.controller;

import com.masterCloudVault.MasterCloudVault.service.K8sDeploymentService;
import com.masterCloudVault.MasterCloudVault.service.TenantDbProvisioningService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class RegistrationController {
    @Autowired
    private TenantDbProvisioningService dbService;
    @Autowired
    private K8sDeploymentService k8sService;

    // ... Constructor Injection


    @PostMapping("/{tenantId}")
    @ResponseStatus(HttpStatus.CREATED)
    public String registerNewTenant(@PathVariable String tenantId) {
        // Generate secure, unique credentials for the tenant's database
        String dbUser = "user_" + tenantId;
        String dbPassword = UUID.randomUUID().toString();


        try {
            // 1. Create Database, User, and Grant Privileges
            dbService.createNewTenantDatabase(tenantId, dbUser, dbPassword);

            // 2. Deploy Tenant Application to Kubernetes
            k8sService.deployTenant(tenantId, dbUser, dbPassword);

            return "Tenant " + tenantId + " provisioned successfully. App starting on K8s.";

        } catch (Exception e) {
            // Add robust error handling (e.g., compensating transactions for rollback)
            return "Provisioning failed: " + e.getMessage();
        }
    }
}
