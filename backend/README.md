# CoCode Backend

## Overview
This is the Scala backend for the CoCode real-time collaborative code editor. It provides RESTful APIs for project management, authentication, and WebSocket endpoints for real-time collaboration.

## Technology Stack
- **Scala 2.13** - Programming language
- **Akka HTTP** - Web framework and WebSocket support
- **MongoDB/PostgreSQL** - Database options for persistence
- **JWT** - Authentication tokens
- **BCrypt** - Password hashing
- **SBT** - Build tool

## Project Structure
```
backend/
├── src/main/scala/com/cocode/
│   ├── Main.scala                 # Application entry point
│   ├── models/                    # Data models
│   │   └── Models.scala
│   ├── routes/                    # HTTP route definitions
│   │   ├── AuthRoutes.scala
│   │   ├── ProjectRoutes.scala
│   │   └── CollaborationRoutes.scala
│   ├── services/                  # Business logic
│   │   ├── AuthService.scala
│   │   ├── ProjectService.scala
│   │   └── CollaborationService.scala
│   └── websocket/                 # WebSocket handling
│       └── WebSocketHandler.scala
├── src/main/resources/
│   └── application.conf           # Configuration file
├── src/test/scala/com/cocode/     # Test files
├── build.sbt                     # SBT build configuration
└── project/
    └── build.properties           # SBT version
```

## Features Implemented

### 1. Authentication & Authorization
- User registration and login
- JWT token-based authentication
- Password hashing with BCrypt
- Token validation and logout

### 2. Project Management
- Create, read, update, delete projects
- File management within projects
- User collaboration permissions
- Project access control

### 3. Real-time Collaboration
- WebSocket connections for real-time updates
- User session management
- Cursor position sharing
- Text change broadcasting
- Collaboration event tracking

### 4. API Endpoints

#### Authentication
- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/logout` - User logout

#### Projects
- `GET /api/v1/projects` - Get user's projects
- `POST /api/v1/projects` - Create new project
- `GET /api/v1/projects/{id}` - Get specific project
- `DELETE /api/v1/projects/{id}` - Delete project
- `GET /api/v1/projects/{id}/files` - Get project files
- `POST /api/v1/projects/{id}/files` - Create new file
- `GET /api/v1/projects/{id}/files/{fileId}` - Get specific file
- `PUT /api/v1/projects/{id}/files/{fileId}` - Update file content
- `DELETE /api/v1/projects/{id}/files/{fileId}` - Delete file

#### Collaboration
- `POST /api/v1/collaboration/sessions/join` - Join collaboration session
- `POST /api/v1/collaboration/sessions/{id}/leave` - Leave session
- `POST /api/v1/collaboration/sessions/{id}/cursor` - Update cursor position
- `GET /api/v1/collaboration/projects/{id}/sessions` - Get active sessions
- `GET /api/v1/collaboration/projects/{id}/events` - Get recent events

#### WebSocket
- `WS /ws/collaborate/{sessionId}` - WebSocket endpoint for real-time collaboration

## Getting Started

### Prerequisites
- Java 8 or higher
- SBT 1.8.2 or higher
- MongoDB or PostgreSQL (optional for development)

### Running the Application
1. Navigate to the backend directory:
   ```bash
   cd backend
   ```

2. Run the application:
   ```bash
   sbt run
   ```

3. The server will start on `http://localhost:8080`

### Configuration
Edit `src/main/resources/application.conf` to configure:
- Database connections
- JWT secret keys
- Server host and port
- WebSocket settings

## Development Notes

### Current Implementation
- Uses in-memory storage for development (replace with actual database)
- Basic conflict resolution (needs operational transformation)
- Simple WebSocket broadcasting (needs connection registry)

### TODO
- [ ] Implement database persistence (MongoDB/PostgreSQL)
- [ ] Add operational transformation for conflict resolution
- [ ] Implement proper WebSocket connection registry
- [ ] Add comprehensive error handling
- [ ] Implement rate limiting
- [ ] Add metrics and monitoring
- [ ] Docker containerization
- [ ] Kubernetes deployment configs

### Testing
Run tests with:
```bash
sbt test
```

## API Usage Examples

### Register a new user
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "john", "email": "john@example.com", "password": "password123"}'
```

### Create a project
```bash
curl -X POST http://localhost:8080/api/v1/projects \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"name": "My Project", "description": "A cool project"}'
```

### Connect to WebSocket
```javascript
const ws = new WebSocket('ws://localhost:8080/ws/collaborate/session-id');
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('Received:', message);
};
```

This backend provides a solid foundation for the CoCode collaborative editor with room for extension and optimization.
