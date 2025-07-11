# TypeScript Server Demo

This example demonstrates how to use the Catenin game engine in a TypeScript Node.js server application with full type safety.

## Features

- **💪 Full TypeScript Support**: Complete type safety using auto-generated `.d.ts` files
- **🏗️ Modern Architecture**: ES6 modules with TypeScript compilation
- **🎮 Game Room Management**: Demonstrates multiplayer game session handling
- **📊 Type-Safe API**: Fully typed interfaces for all game operations
- **🔧 Development Tools**: Hot reload, source maps, and debugging support
- **🎯 Production Ready**: Compiled JavaScript output for deployment

## Prerequisites

- Node.js 18+ (recommended)
- npm or yarn package manager

## Installation

```bash
# Navigate to this directory
cd catenin/examples/typescript-server-demo

# Install dependencies
npm install
```

## Running the Demo

```bash
# Development mode (with hot reload)
npm run dev

# Build and run production version
npm run build
npm start

# Type checking only
npm run type-check
```

## Project Structure

```
typescript-server-demo/
├── server.ts           # TypeScript server implementation
├── package.json        # Dependencies and scripts
├── tsconfig.json       # TypeScript configuration
├── dist/              # Compiled JavaScript output
└── README.md          # This file
```

## How it Works

The demo showcases:

1. **Type-Safe Library Import**: Uses generated TypeScript definitions
2. **Game Room Management**: Creates and manages multiple game sessions
3. **Player State Tracking**: Monitors game state with full type safety
4. **Card Generation**: Demonstrates card factory with typed properties
5. **Error Handling**: Proper TypeScript error management

## TypeScript Features Demonstrated

### Import with Type Safety
```typescript
import { 
  GameEngine, 
  GameDefinitionParser, 
  CardFactory, 
  createGameEngineFromYaml, 
  Player, 
  Card, 
  GameDefinition 
} from '../../../build/dist/js/developmentLibrary/junction-catenin.mjs';
```

### Typed Interfaces
```typescript
interface GameRoom {
  id: string;
  players: string[];
  engine: GameEngine;
  createdAt: Date;
}
```

### Type-Safe Operations
```typescript
const players: Player[] = gameEngine.getPlayers();
const cards: Card[] = cardFactory.generateCards();
const definition: GameDefinition = gameEngine.getGameDefinition();
```

### Generic Methods
```typescript
getGamesByPlayerCount<T extends number>(playerCount: T): GameRoom[] {
  return this.getAllRooms().filter(room => room.players.length === playerCount);
}
```

## Configuration

### TypeScript Configuration
The `tsconfig.json` includes:
- **ES2022 target**: Modern JavaScript features
- **ESNext modules**: Full ES6 module support
- **Strict type checking**: Maximum type safety
- **Source maps**: Full debugging support

### Build Scripts
- `npm run dev`: Development with ts-node
- `npm run build`: Compile to JavaScript
- `npm start`: Run compiled version
- `npm run build:lib`: Build the Catenin library
- `npm run type-check`: Type checking only

## Browser Compatibility

The compiled JavaScript works with:
- Node.js 18+
- ES6 module environments
- TypeScript projects
- Modern bundlers (Webpack, Rollup, etc.)

## Use Cases

This TypeScript setup is ideal for:
- **Enterprise Applications**: Full type safety and IntelliSense
- **Large Teams**: Better code maintainability and documentation
- **API Servers**: Type-safe REST/GraphQL APIs
- **Real-time Services**: WebSocket servers with typed messages
- **Microservices**: Scalable game backend services
- **Development Tools**: IDE support and refactoring safety

## Integration Example

### Express.js Server
```typescript
import express from 'express';
import { TypeScriptGameServer } from './server.js';

const app = express();
const gameServer = new TypeScriptGameServer();

app.post('/games', (req, res) => {
  const roomId = gameServer.createGameRoom(req.body.players);
  res.json({ roomId });
});

app.get('/games/:roomId', (req, res) => {
  const room = gameServer.getGameRoom(req.params.roomId);
  if (!room) {
    return res.status(404).json({ error: 'Room not found' });
  }
  res.json(room);
});
```

### WebSocket Server
```typescript
import { WebSocketServer } from 'ws';
import { TypeScriptGameServer } from './server.js';

const wss = new WebSocketServer({ port: 8080 });
const gameServer = new TypeScriptGameServer();

wss.on('connection', (ws) => {
  ws.on('message', (data) => {
    const message = JSON.parse(data.toString());
    // Handle game actions with full type safety
  });
});
```

## Development

### Adding New Features
1. Update interfaces in `server.ts`
2. TypeScript will catch type errors automatically
3. Use `npm run type-check` to validate
4. Test with `npm run dev`

### Production Deployment
1. `npm run build` - Compile to JavaScript
2. Deploy `dist/` folder
3. Run with `node dist/server.js`

## Dependencies

- **TypeScript**: Type-safe JavaScript compilation
- **@types/node**: Node.js type definitions
- **ts-node**: TypeScript execution for development

## External Usage

To use this pattern in your own TypeScript projects:

1. Copy the build script: `npm run build:lib`
2. Install TypeScript dependencies
3. Import with full type safety
4. Use the generated `.d.ts` files for IntelliSense

## Performance

- **Development**: Hot reload with ts-node
- **Production**: Compiled JavaScript (no TypeScript runtime overhead)
- **Memory**: Efficient compiled output
- **Type Safety**: Zero runtime cost for type checking

---

**TypeScript + Catenin** - *Enterprise-grade game development with full type safety* 🎮✨