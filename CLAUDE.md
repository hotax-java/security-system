# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Security System** is a comprehensive enterprise-grade authentication and authorization system built on Spring Boot + Spring Security + OAuth2. It provides complete user authentication, permission management, and single sign-on (SSO) capabilities.

### Architecture

```
security-system/
├── security-core/        # Core entities, services, and utilities
├── security-sso/         # OAuth2 authorization server (Port: 9000/9001)
├── security-admin/       # Permission management backend (Port: 8080)
└── security-admin/ui/    # React frontend (Port: 3000)
```

## Quick Start Commands

### Development Environment Setup

```bash
# 1. Database setup (MySQL required)
mysql -u root -p < security-sso/src/main/resources/db/migration/V2__Create_OAuth2_Tables.sql
mysql -u root -p < security-admin/src/main/resources/db/migration/V1__create_tables_and_initial_data.sql

# 2. Start OAuth2 authorization server
cd security-sso
mvn spring-boot:run

# 3. Start permission management backend
cd security-admin
mvn spring-boot:run

# 4. Start frontend development server
cd security-admin/ui
npm install
npm start
```

### Production Deployment

```bash
# Build all modules
mvn clean package -DskipTests

# Start services using Docker
docker-compose up -d
```

### Service URLs

- **Frontend**: http://localhost:3000 (admin/admin123, user/user123)
- **SSO Server**: http://localhost:9000
- **Admin API**: http://localhost:8080

## Key Configuration Files

### SSO Service (security-sso)
- `src/main/resources/application.yml` - SSO server configuration
- `src/main/java/com/webapp/security/sso/config/SecurityConfig.java` - OAuth2 security config
- `src/main/java/com/webapp/security/sso/config/JwtConfig.java` - JWT token configuration

### Admin Service (security-admin)
- `src/main/resources/application.yml` - Admin backend configuration
- `src/main/java/com/webapp/security/admin/config/SecurityConfig.java` - Resource server config
- `src/main/java/com/webapp/security/admin/controller/` - REST API endpoints

### Frontend (security-admin/ui)
- `src/services/authService.ts` - Authentication service
- `src/components/common/Permission.tsx` - Permission control component
- `src/utils/permissionUtil.ts` - Permission utility functions

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.2.3, Spring Security 6.2.1
- **Security**: Spring Security OAuth2 1.2.0, JWT 0.11.5
- **Database**: MySQL 8.0.33, MyBatis-Plus 3.5.12, Flyway
- **Tools**: Lombok, MapStruct, Hutool, Guava

### Frontend
- **Framework**: React 18.2, TypeScript 4.9
- **UI**: Ant Design 5.26
- **Build**: Create React App, Vite

## Module Responsibilities

### security-core
- **Purpose**: Shared entities, services, and utilities
- **Key Classes**:
  - `entity/` - Database entities (User, Role, Permission, etc.)
  - `service/` - Core business logic services
  - `mapper/` - MyBatis-Plus data access layer

### security-sso
- **Purpose**: OAuth2 authorization server + SSO
- **Key Classes**:
  - `OAuth2Controller.java` - OAuth2 endpoints
  - `UserDetailsServiceImpl.java` - User authentication service
  - `MyBatisOAuth2AuthorizationService.java` - OAuth2 persistence
  - `OAuth2RegisteredClientService.java` - Client registration

### security-admin
- **Purpose**: Permission management backend (resource server)
- **Key Classes**:
  - `UserController.java` - User management API
  - `RoleController.java` - Role management API
  - `PermissionController.java` - Permission management API

### security-admin/ui
- **Purpose**: Frontend management interface
- **Key Features**:
  - User, role, and permission management
  - Real-time permission validation
  - Responsive design with Ant Design

## Database Schema

### Core Tables
- `sys_user` - User accounts
- `sys_role` - Role definitions
- `sys_permission` - Permission definitions (3 types: menu, button, API)
- `sys_user_role` - User-role relationships
- `sys_role_permission` - Role-permission relationships

### OAuth2 Tables
- `oauth2_registered_client` - OAuth2 client registrations
- `oauth2_authorization` - Authorization records
- `oauth2_jwk` - JWT signing keys

## Permission System

### Permission Types
- **Menu permissions** (perm_type=1) - Control navigation menu visibility
- **Button permissions** (perm_type=2) - Control action buttons
- **API permissions** (perm_type=3) - Control backend API access

### Permission Hierarchy
```
System Management
├── User Management
│   ├── user:view
│   ├── user:add
│   ├── user:edit
│   └── user:delete
└── Role Management
    ├── role:view
    ├── role:add
    └── role:edit
```

### Permission Validation
- **JWT-based**: Permissions embedded in JWT tokens
- **Frontend**: `permissionUtil.hasPermission('USER_VIEW')`
- **Backend**: `@PreAuthorize("hasAuthority('USER_VIEW')")`

## API Endpoints

### SSO Service (Port: 9000)
- `POST /oauth2/login` - User login
- `POST /oauth2/token` - Token exchange
- `GET /.well-known/jwks.json` - JWK public keys
- `POST /oauth2/revoke` - Token revocation

### Admin Service (Port: 8080)
- `GET /api/users` - List users (requires: user:view)
- `POST /api/users` - Create user (requires: user:add)
- `PUT /api/users/{id}` - Update user (requires: user:edit)
- `DELETE /api/users/{id}` - Delete user (requires: user:delete)
- `GET /api/dashboard/menus` - Get user menu permissions

## Development Workflow

### Adding New Permissions
1. Add permission to `sys_permission` table
2. Assign to roles via `sys_role_permission`
3. Use in frontend: `<Permission code="NEW_PERM">...</Permission>`
4. Use in backend: `@PreAuthorize("hasAuthority('NEW_PERM')")`

### Adding New OAuth2 Clients
1. Insert into `oauth2_registered_client` table
2. Configure redirect URIs and scopes
3. Test with OAuth2 flow

### Testing
```bash
# Backend tests
mvn test

# Frontend tests
cd security-admin/ui
npm test

# Integration tests
# Use provided test scripts in security-sso/src/main/resources/test/
```

## Common Issues & Solutions

### Database Connection Issues
- Ensure MySQL is running and accessible
- Check `application.yml` database configuration
- Verify Flyway migrations completed successfully

### JWT Signature Issues
- Ensure SSO server is running on expected port
- Check JWK endpoint: `http://localhost:9000/.well-known/jwks.json`
- Verify JWT token issuer matches expected value

### Frontend Permission Issues
- Check JWT token contains `authorities` claim
- Verify permission codes match exactly (case-sensitive)
- Clear browser cache if permissions updated

## Build Commands

```bash
# Full build
mvn clean install

# Skip tests
mvn clean install -DskipTests

# Build with profiles
mvn clean install -Pprod

# Frontend build
cd security-admin/ui
npm run build
```

## Environment Configuration

### Local Development
```yaml
# SSO service application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/security_system
    username: root
    password: your_password

# Frontend .env.development
REACT_APP_API_BASE_URL=http://localhost:8080
REACT_APP_AUTH_BASE_URL=http://localhost:9000
```

### Production
- Use environment variables for sensitive data
- Configure proper CORS origins
- Enable HTTPS/TLS
- Use production-grade database connections