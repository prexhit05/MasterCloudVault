# MasterCloudVault – SaaS Tenant Orchestrator (POC)

MasterCloudVault is a **Spring Boot POC** demonstrating the 
**Separate Service Instance per Tenant** SaaS model. For every new tenant, it provisions an **isolated MySQL database**,
stores metadata in a **master database**, and deploys a **tenant-specific Kubernetes Deployment + Service**.
---

## 🚀 Features

### ✔️ Master Application

* Built using **Spring Boot 3** / **Java 17**.
* Stores tenant metadata using **JPA**.
* Provides an HTTP API to register new tenants.

### ✔️ Tenant Database Provisioning

* Creates isolated databases: `db_<tenantId>`.
* Creates MySQL users with unique credentials.
* Grants privileges per tenant.
* Persists database metadata in the master DB.

### ✔️ Kubernetes Deployment per Tenant

* Uses the **Fabric8 Kubernetes Client**.
* Generates deployment & service YAML from templates.
* Injects tenant-specific values:

  * `${TENANT_ID}`
  * `${DB_URL}`
  * `${DB_USERNAME}`
  * `${DB_PASSWORD}`
  * `${TENANT_APP_IMAGE}`
* Creates **Deployment + Service** for each tenant.

---

## 🏗️ Architecture Overview

* **Master App** manages onboarding and metadata.
* **MySQL** root connection performs tenant DB provisioning.
* **Kubernetes** cluster runs isolated tenant instances.

```
Client → Master App → MySQL Provisioning → K8s Deployment → Tenant Pod Running
```

---

## 🔧 Prerequisites

* Java 17+
* Maven (or `mvnw` wrapper)
* MySQL server (with privileges to create DBs and users)
* Kubernetes cluster
* Kubeconfig configured (Fabric8 uses local config)

---

## ⚙️ Configuration

Main config file: `src/main/resources/application.yaml`

Supports:

* Master DB datasource
* MySQL provisioning credentials
* Kubernetes template variables

**Tenant DB URL format:**

```
jdbc:mysql://<mysql.provisioning.host>:3306/db_<tenantId>?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC
```

---

## ▶️ Running the Master App

### Run

```bash
./mvnw spring-boot:run
```

### Run tests

```bash
./mvnw test
```

Default port: **[http://localhost:8080](http://localhost:8080)**

---

## 🧩 Tenant Registration API

### Endpoint

```
POST /{tenantId}
```

**Path variable:** `tenantId` (example: `acme` or `tenant123`)

### What Happens Internally

1. Generates DB credentials.
2. Creates new MySQL database + user.
3. Saves metadata in master DB.
4. Renders Kubernetes YAML.
5. Applies Deployment + Service to cluster.

### Example

```bash
curl -X POST http://localhost:8080/acme
```

**Response:**

```
Tenant acme provisioned successfully. App starting on K8s.
```

---

## 📦 Kubernetes Templates

Location: `src/main/resources/k8s-templates/`

### deployment-template.yaml

* Deployment name: `cloudvault-${TENANT_ID}`
* Env vars:

  * TENANT_ID
  * DB_URL
  * DB_USERNAME
  * DB_PASSWORD
  * TENANT_APP_IMAGE

### service-template.yaml

* Service name: `service-${TENANT_ID}`
* Selects pods with `tenant: ${TENANT_ID}`
* Exposes port 80 → 8080

---

## 🔍 Observing Tenant Deployments

```bash
kubectl get pods
kubectl get svc
```

Look for:

* Pods: `cloudvault-<tenantId>-...`
* Services: `service-<tenantId>`

View pod logs:

```bash
kubectl logs <pod-name>
```

---

## ⚠️ Notes & Limitations

This is a **POC**, not production ready.

### Current Limitations

* Tenant DB passwords stored in plaintext.
* No rollback on failure.
* No advanced Kubernetes error handling.

### For Production

* Use Kubernetes **Secrets**.
* Implement retry/rollback.
* Add monitoring, quotas, isolation.

---

## 📁 Project Structure

```
src/main/java/com/masterCloudVault/MasterCloudVault/
  MasterCloudVaultApplication.java
  controller/
    RegistrationController.java
  entity/
    MasterEntity.java
  repo/
    MasterEntityRepo.java
  service/
    TenantDbProvisioningService.java
    K8sDeploymentService.java

src/main/resources/
  application.yaml
  k8s-templates/
    deployment-template.yaml
    service-template.yaml

src/test/java/...
```

---

## 📜 License

This POC is free to modify and use. Provided **as-is** for demo purposes.

---

Made with ❤️ for scalable multi-tenant architectures.
