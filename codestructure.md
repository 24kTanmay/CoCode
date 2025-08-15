# CoCode Project Structure

## 📁 Project Architecture Overview

```
CoCode/
├── 📄 Project.md                          # Project planning and features
├── 📁 backend/                            # Scala backend application
│   ├── 📄 build.sbt                       # SBT build configuration
│   ├── 📄 Dockerfile                      # Docker container config
│   ├── 📄 .gitignore                      # Git ignore rules
│   ├── 📄 README.md                       # Backend documentation
│   ├── 📄 WEBSOCKET_IMPLEMENTATION.md     # WebSocket feature docs
│   ├── 📄 websocket-test.html             # WebSocket testing client
│   │
│   ├── 📁 project/                        # SBT project configuration
│   │   └── 📄 build.properties            # SBT version specification
│   │
│   ├── 📁 src/                            # Source code directory
│   │   ├── 📁 main/                       # Main application code
│   │   │   ├── 📁 scala/com/cocode/       # Scala source packages
│   │   │   │   ├── 📄 Main.scala          # Application entry point
│   │   │   │   │
│   │   │   │   ├── 📁 models/             # Data models
│   │   │   │   │   └── 📄 Models.scala    # Core data structures
│   │   │   │   │
│   │   │   │   ├── 📁 routes/             # HTTP route handlers
│   │   │   │   │   ├── 📄 AuthRoutes.scala           # Authentication endpoints
│   │   │   │   │   ├── 📄 ProjectRoutes.scala        # Project management endpoints
│   │   │   │   │   ├── 📄 CollaborationRoutes.scala  # Collaboration endpoints
│   │   │   │   │   └── 📄 WebSocketRoutes.scala      # WebSocket management endpoints
│   │   │   │   │
│   │   │   │   ├── 📁 services/           # Business logic layer
│   │   │   │   │   ├── 📄 AuthService.scala          # Authentication service
│   │   │   │   │   ├── 📄 ProjectService.scala       # Project management service
│   │   │   │   │   └── 📄 CollaborationService.scala # Collaboration service
│   │   │   │   │
│   │   │   │   ├── 📁 actors/             # Actor system for real-time features
│   │   │   │   │   ├── 📄 CollaborationManager.scala # Central collaboration coordinator
│   │   │   │   │   ├── 📄 ProjectSessionActor.scala  # Project-specific session manager
│   │   │   │   │   └── 📄 ConnectionActor.scala      # Individual connection handler
│   │   │   │   │
│   │   │   │   └── 📁 websocket/          # WebSocket infrastructure
│   │   │   │       ├── 📄 WebSocketHandler.scala     # WebSocket route handler
│   │   │   │       └── 📄 WebSocketManager.scala     # Type definitions
│   │   │   │
│   │   │   └── 📁 resources/              # Configuration files
│   │   │       └── 📄 application.conf    # Application configuration
│   │   │
│   │   └── 📁 test/                       # Test code directory
│   │       └── 📁 scala/com/cocode/       # Test packages (structure mirrors main)
│   │
│   └── 📁 target/                         # Build output (generated, not in Git)
│       └── ...                            # Compiled classes and artifacts
```

## 🏗️ Architecture Layers

### **1. Application Layer**
```
📄 Main.scala
├── HTTP Server Setup (Akka HTTP)
├── Route Configuration
├── Actor System Initialization
└── Application Lifecycle Management
```

### **2. API Layer (Routes)**
```
📁 routes/
├── 🔐 AuthRoutes.scala           → /api/v1/auth/*
├── 📁 ProjectRoutes.scala        → /api/v1/projects/*
├── 🤝 CollaborationRoutes.scala  → /api/v1/collaboration/*
└── 🔌 WebSocketRoutes.scala      → /api/v1/websocket/*
```

### **3. Business Logic Layer (Services)**
```
📁 services/
├── 🔐 AuthService.scala          → User authentication & JWT
├── 📁 ProjectService.scala       → Project & file management
└── 🤝 CollaborationService.scala → Real-time collaboration logic
```

### **4. Real-Time Layer (Actors)**
```
📁 actors/
├── 🎯 CollaborationManager.scala  → Central coordinator
│   ├── Manages multiple projects
│   ├── Routes messages to projects
│   └── Handles user connections
│
├── 🏢 ProjectSessionActor.scala   → Project-specific manager
│   ├── Manages users in one project
│   ├── Broadcasts messages to users
│   └── Handles join/leave events
│
└── 👤 ConnectionActor.scala       → Individual user connection
    ├── Handles one WebSocket connection
    ├── Processes incoming messages
    └── Sends outgoing messages
```

### **5. WebSocket Layer**
```
📁 websocket/
├── 🔌 WebSocketHandler.scala     → WebSocket route handling
│   ├── Connection establishment
│   ├── Authentication validation
│   └── Message flow management
│
└── 📝 WebSocketManager.scala     → Type definitions
    └── Common types and utilities
```

### **6. Data Layer (Models)**
```
📁 models/
└── 📄 Models.scala
    ├── User                      → User account data
    ├── Project                   → Project metadata
    ├── CodeFile                  → File content and info
    ├── CodeChange                → Edit operations
    ├── UserSession               → Active user sessions
    └── CollaborationEvent        → Real-time events
```

## 🌐 API Endpoint Structure

### **Authentication Endpoints**
```
POST /api/v1/auth/register     → AuthRoutes.scala → AuthService.scala
POST /api/v1/auth/login        → AuthRoutes.scala → AuthService.scala
POST /api/v1/auth/logout       → AuthRoutes.scala → AuthService.scala
```

### **Project Management Endpoints**
```
GET    /api/v1/projects                    → ProjectRoutes.scala → ProjectService.scala
POST   /api/v1/projects                    → ProjectRoutes.scala → ProjectService.scala
GET    /api/v1/projects/{id}               → ProjectRoutes.scala → ProjectService.scala
DELETE /api/v1/projects/{id}               → ProjectRoutes.scala → ProjectService.scala
GET    /api/v1/projects/{id}/files         → ProjectRoutes.scala → ProjectService.scala
POST   /api/v1/projects/{id}/files         → ProjectRoutes.scala → ProjectService.scala
GET    /api/v1/projects/{id}/files/{fileId} → ProjectRoutes.scala → ProjectService.scala
PUT    /api/v1/projects/{id}/files/{fileId} → ProjectRoutes.scala → ProjectService.scala
DELETE /api/v1/projects/{id}/files/{fileId} → ProjectRoutes.scala → ProjectService.scala
```

### **Collaboration Endpoints**
```
POST /api/v1/collaboration/sessions/join           → CollaborationRoutes.scala → CollaborationService.scala
POST /api/v1/collaboration/sessions/{id}/leave     → CollaborationRoutes.scala → CollaborationService.scala
POST /api/v1/collaboration/sessions/{id}/cursor    → CollaborationRoutes.scala → CollaborationService.scala
GET  /api/v1/collaboration/projects/{id}/sessions  → CollaborationRoutes.scala → CollaborationService.scala
GET  /api/v1/collaboration/projects/{id}/events    → CollaborationRoutes.scala → CollaborationService.scala
```

### **WebSocket Endpoints**
```
WS /ws/collaborate/{projectId}?token={jwt} → WebSocketHandler.scala → Actor System
```

## 🔄 Data Flow Architecture

### **HTTP Request Flow**
```
Client Request
    ↓
Main.scala (Route Matching)
    ↓
Route Handler (AuthRoutes/ProjectRoutes/CollaborationRoutes)
    ↓
Service Layer (AuthService/ProjectService/CollaborationService)
    ↓
Data Models (User/Project/CodeFile)
    ↓
Response to Client
```

### **WebSocket Message Flow**
```
WebSocket Client
    ↓
WebSocketHandler.scala (Authentication & Routing)
    ↓
CollaborationManager.scala (Central Coordinator)
    ↓
ProjectSessionActor.scala (Project-Specific Manager)
    ↓
ConnectionActor.scala (Individual Connection)
    ↓
Broadcast to Other Users in Project
```

## 🧪 Testing & Development Files

### **Testing Infrastructure**
```
📄 websocket-test.html           → Comprehensive WebSocket testing client
    ├── Connection testing
    ├── Message broadcasting
    ├── Real-time collaboration simulation
    └── Debug and monitoring tools
```

### **Documentation**
```
📄 README.md                    → Backend documentation and setup
📄 WEBSOCKET_IMPLEMENTATION.md  → WebSocket feature documentation
📄 Project.md                   → Overall project planning
```

### **Configuration**
```
📄 application.conf              → Server, database, and feature configuration
📄 build.sbt                    → Dependencies and build settings
📄 Dockerfile                   → Container configuration
📄 .gitignore                   → Git ignore patterns
```

## 🚀 Deployment Structure

### **Container Ready**
```
Dockerfile
├── Java 11 Runtime
├── SBT Build Tool
├── Application Compilation
├── Port 8080 Exposure
└── Health Check Configuration
```

### **Build Configuration**
```
build.sbt
├── Scala 2.13 Configuration
├── Akka HTTP Dependencies
├── Database Drivers (MongoDB/PostgreSQL)
├── Authentication Libraries (JWT/BCrypt)
├── JSON Processing (Spray JSON)
└── Testing Dependencies
```

## 📊 File Count Summary

| Category | Files | Description |
|----------|-------|-------------|
| **Scala Source** | 12 files | Core application logic |
| **Configuration** | 3 files | Build and runtime config |
| **Documentation** | 3 files | Project and API docs |
| **Testing** | 1 file | WebSocket test client |
| **Build & Deploy** | 3 files | Build, Docker, Git config |
| **Total** | **22 files** | Complete implementation |

## 🎯 Key Architectural Patterns

1. **Layered Architecture**: Clear separation between routes, services, and models
2. **Actor Pattern**: Real-time collaboration using Akka actors
3. **Microservice Ready**: Stateless design for horizontal scaling
4. **Event-Driven**: WebSocket-based real-time messaging
5. **Security First**: JWT authentication throughout
6. **Test-Driven**: Comprehensive testing infrastructure

This structure provides a solid foundation for a scalable, real-time collaborative code editor! 🎉
