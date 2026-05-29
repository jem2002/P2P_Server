# Library Information Recommendation System

A comprehensive microservices-based system built with .NET 10 for managing library information and providing book recommendations.

## Architecture Overview

This project is organized into two main microservices:

- **AuthService**: Handles authentication, user registration, login, and JWT token generation
- **CatalogService**: Manages the library catalog and book recommendations

### Technology Stack
- **Backend**: ASP.NET Core 10
- **Databases**: PostgreSQL (Catalog), Redis (Auth Cache)
- **Container Orchestration**: Docker & Docker Compose
- **Testing**: Unit and Integration Tests

---

## Prerequisites

Before running the project, ensure you have installed:

- [.NET 10 SDK](https://dotnet.microsoft.com/en-us/download)
- [Docker & Docker Compose](https://www.docker.com/products/docker-desktop)
- [Git](https://git-scm.com)
- A code editor (Visual Studio, Visual Studio Code, or Rider)

### Optional Tools
- [Postman](https://www.postman.com) - for API testing
- [DBeaver](https://dbeaver.io) - for database management

---

## Project Structure

```
Library-Information-Recommendation-System/
├── src/
│   ├── AuthService/
│   │   ├── MiniIdentityApi.Api/          # API endpoints
│   │   ├── MiniIdentityApi.Application/  # DTOs & Services
│   │   ├── MiniIdentityApi.Domain/       # Domain entities
│   │   ├── MiniIdentityApi.Infrastructure/  # Repositories & Security
│   │   └── MiniIdentityApi.Tests/        # Unit & Integration tests
│   │
│   └── CatalogService/
│       ├── Catalog.Api/                   # API endpoints
│       ├── Catalog.Application/           # DTOs & Services
│       ├── Catalog.Domain/                # Domain entities
│       └── Catalog.Infrastructure/        # Repositories & Database
│
├── docs/                                  # Documentation & UML diagrams
├── postman/                               # Postman collections
├── docker-compose.yml                     # Services configuration
└── README.md
```

---

## Running the Project

### Option 1: Quick Start with Docker Compose (Recommended)

This is the fastest way to get the entire system running with all dependencies.

#### Step 1: Start the Database Services

```bash
docker-compose up -d
```

This command will start:
- **Redis** (port 6379) - for AuthService caching
- **PostgreSQL** (port 5435) - for CatalogService database

Verify the services are running:
```bash
docker-compose ps
```

#### Step 2: Build the .NET Services

From the project root, build all services:

```bash
dotnet build
```

#### Step 3: Run the AuthService

Open a new terminal and navigate to the AuthService project:

```bash
cd src/AuthService/MiniIdentityApi.Api
dotnet run
```

The AuthService will be available at `https://localhost:5001` (or the port shown in console)

#### Step 4: Run the CatalogService

Open another terminal and navigate to the CatalogService project:

```bash
cd src/CatalogService/Catalog.Api
dotnet run
```

The CatalogService will be available at `https://localhost:5002` (or the port shown in console)

#### Step 5: Access the APIs

Both services include Swagger UI for easy testing:

- **AuthService Swagger**: https://localhost:5001/swagger
- **CatalogService Swagger**: https://localhost:5002/swagger

---

### Option 2: Running Services Locally (Manual Setup)

If you prefer to run services without Docker, you'll need to manually configure the databases.

#### AuthService Setup

1. Navigate to the AuthService directory:
```bash
cd src/AuthService/MiniIdentityApi.Api
```

2. Install dependencies:
```bash
dotnet restore
```

3. Configure Redis (if using cache features):
   - Install Redis locally or skip if not using Auth cache
   - Update connection strings in `appsettings.Development.json` if needed

4. Run the service:
```bash
dotnet run --configuration Development
```

#### CatalogService Setup

1. Navigate to the CatalogService directory:
```bash
cd src/CatalogService/Catalog.Api
```

2. Install dependencies:
```bash
dotnet restore
```

3. Configure PostgreSQL:
   - Ensure PostgreSQL is running locally
   - Update the connection string in `appsettings.Development.json`:
   ```json
   {
     "ConnectionStrings": {
       "DefaultConnection": "Host=localhost;Port=5432;Database=catalog_db;Username=postgres;Password=your_password"
     }
   }
   ```

4. Apply database migrations (if applicable):
```bash
dotnet ef database update
```

5. Run the service:
```bash
dotnet run --configuration Development
```

---

## Database Configuration

### PostgreSQL (CatalogService)

**Docker Configuration** (from docker-compose.yml):
- Host: `localhost`
- Port: `5435`
- Database: `catalog_db`
- Username: `postgres`
- Password: `secret-postgres`

**Connection String**:
```
Host=localhost;Port=5435;Database=catalog_db;Username=postgres;Password=secret-postgres
```

### Redis (AuthService)

**Docker Configuration** (from docker-compose.yml):
- Host: `localhost`
- Port: `6379`
- Password: `secret-redis`

**Connection String**:
```
localhost:6379,password=secret-redis
```

---

## Testing the APIs

### Using Postman

1. Import the provided Postman collections:
   - `postman/MiniIdentityApi.postman_collection.json` - AuthService endpoints
   - `postman/MiniIdentityApi.postman_environment.json` - Environment variables

2. Update environment variables if needed:
   - `baseUrl`: Your local API URL
   - `jwt`: Token from login (auto-filled after login request)
   - `port`: API port number

### Using Swagger UI

1. Open your browser and navigate to the Swagger URL of each service
2. Click "Try it out" on any endpoint
3. Fill in the required parameters
4. Click "Execute"

### Example Workflow

#### 1. Register a User (AuthService)
```
POST /api/auth/register
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

#### 2. Login
```
POST /api/auth/login
{
  "usernameOrEmail": "john_doe",
  "password": "SecurePass123!"
}
```

Response will include a JWT token. Copy this token.

#### 3. Access Protected Endpoints
```
GET /api/demo/profile
Authorization: Bearer <your_jwt_token>
```

---

## Running Tests

### Run All Tests

```bash
dotnet test
```

### Run Tests for Specific Project

```bash
dotnet test src/AuthService/MiniIdentityApi.Tests/MiniIdentityApi.Tests.csproj
```

### Run Tests with Coverage

```bash
dotnet test /p:CollectCoverage=true
```

---

## Troubleshooting

### Port Already in Use
If ports 5001, 5002, or the database ports are already in use:

1. **Find the process using the port**:
   ```bash
   # On Windows
   netstat -ano | findstr :5001
   
   # On macOS/Linux
   lsof -i :5001
   ```

2. **Kill the process** or change the port in the service configuration

### Docker Container Issues

**Check container logs**:
```bash
docker-compose logs -f
```

**Restart containers**:
```bash
docker-compose restart
```

**Stop all containers**:
```bash
docker-compose down
```

### Database Connection Failed

1. Verify the connection string matches your configuration
2. Ensure the database server is running
3. Check firewall settings aren't blocking connections
4. Verify database credentials are correct

### .NET Build Issues

1. Clean the build:
```bash
dotnet clean
```

2. Restore packages:
```bash
dotnet restore
```

3. Rebuild:
```bash
dotnet build
```

---

## Documentation

- **Architecture Diagram**: `docs/uml/mini-identity-api-uml.png`
- **API Documentation**: Available at `/swagger` endpoints
- **Service Details**: See sections below

---

## AuthService Components

### Endpoints
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and receive JWT token
- `GET /api/users` - List all users (Admin only)
- `POST /api/roles` - Create roles (Admin only)

### Key Features
- JWT authentication
- Password hashing with SHA-256
- Role-based access control
- User management
- Permission assignment

For detailed information, see the AuthService documentation in the project.

---

## CatalogService Components

### Endpoints
- `GET /api/catalog/books` - Get all books
- `GET /api/catalog/books/{id}` - Get book by ID
- `POST /api/catalog/books` - Create new book (Admin only)
- `GET /api/recommendations` - Get personalized recommendations

### Key Features
- Book catalog management
- Recommendation engine
- PostgreSQL persistence
- RESTful API design

---

## Stopping the Services

### Stop All Docker Containers

```bash
docker-compose down
```

### Remove All Data (Start Fresh)

```bash
docker-compose down -v
```

---

## Support & Documentation

For more information on specific services, check the documentation in the `docs/` folder.

For detailed API endpoints and request/response formats, visit the Swagger UI when services are running.

---

## License

This project is provided as-is for educational purposes.
