package com.masterCloudVault.MasterCloudVault.service;

import com.masterCloudVault.MasterCloudVault.entity.MasterEntity;
import com.masterCloudVault.MasterCloudVault.repo.MasterEntityRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantDbProvisioningService {

    private final MasterEntityRepo repo;

    private final JdbcTemplate provisioningJdbcTemplate;
    // Inject the provisioning configuration (root/admin connection)

    public MasterEntity createNewTenantDatabase(String tenantId, String dbUser, String dbPassword) {
        // Sanitize the tenantId for safe use in SQL
        String safeDbName = "db_" + tenantId.replaceAll("[^a-zA-Z0-9_]", "");

        // 1. Create the dedicated database
        String createDbSql = "CREATE DATABASE IF NOT EXISTS " + safeDbName +
                " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;";
        provisioningJdbcTemplate.execute(createDbSql);

        // 2. Create the dedicated database user (MySQL)
        // Note: In production, the user is often pre-created or managed by a service.
        String createUserSql = String.format("CREATE USER '%s'@'%%' IDENTIFIED BY '%s';", dbUser, dbPassword);
        provisioningJdbcTemplate.execute(createUserSql);

        // 3. Grant privileges to the new user on the new database only
        String grantPrivilegesSql = String.format("GRANT ALL PRIVILEGES ON %s.* TO '%s'@'%%';", safeDbName, dbUser);
        provisioningJdbcTemplate.execute(grantPrivilegesSql);

        // 4. Reload privileges
        provisioningJdbcTemplate.execute("FLUSH PRIVILEGES;");


        MasterEntity masterEntity = new MasterEntity();
        masterEntity.setTenantId(tenantId);
        masterEntity.setDbname(safeDbName);
        masterEntity.setDbUser(dbUser);
        masterEntity.setDbPassword(dbPassword);

        repo.save(masterEntity);

        return masterEntity;

        // Log the credentials securely
        // Persist the new tenant's metadata (dbUser, dbPassword, dbName) in the Master Database
    }

}
