import { GameEngine, CardFactory, createGameEngineFromYaml, Player, Card, GameDefinition } from '@junction/catenin';

interface GameRoom {
    id: string;
    players: string[];
    engine: GameEngine;
    createdAt: Date;
}

class TypeScriptGameServer {
    private games: Map<string, GameRoom> = new Map();
    private nextRoomId: number = 1;

    start(): void {
        console.log('🎮 TypeScript Game Server Starting...');
        console.log('🚀 Server ready to handle game sessions');
        
        // Create demo game rooms
        this.createGameRoom(['Alice', 'Bob']);
        this.createGameRoom(['Charlie', 'Diana', 'Eve']);
        
        // Demonstrate server functionality
        this.demonstrateGameManagement();
    }

    createGameRoom(playerNames: string[]): string {
        const roomId = `room_${this.nextRoomId++}`;
        console.log(`🏠 Creating game room: ${roomId}`);
        console.log(`👥 Players: ${playerNames.join(', ')}`);

        // Define game using YAML
        const yamlContent = `
meta:
  name: "TypeScript Server Demo Game"
  target_age: [10, 16]
  player_count: [2, 4]

cards:
  attack_card:
    count: 15
    properties:
      damage: {type: int, min: 3, max: 7}
      element: {type: enum, values: [fire, water, earth, air]}
      
  defense_card:
    count: 10
    properties:
      shield: {type: int, min: 2, max: 5}
      
  spell_card:
    count: 8
    properties:
      effect: {type: enum, values: [heal, boost, curse]}
      power: {type: int, min: 1, max: 4}

mechanics:
  setup:
    players:
      health: 25
      hand_size: 6
  win_conditions:
    - type: "health_depleted"
      message: "{winner} claims victory!"
        `.trim();

        try {
            // Create game engine with TypeScript type safety
            const gameEngine: GameEngine = createGameEngineFromYaml(yamlContent, playerNames);
            
            const gameRoom: GameRoom = {
                id: roomId,
                players: playerNames,
                engine: gameEngine,
                createdAt: new Date()
            };
            
            this.games.set(roomId, gameRoom);
            
            console.log('📄 Game definition parsed successfully');
            console.log(`🎮 Game engine created for room ${roomId}`);
            
            // Display game information with TypeScript types
            const definition: GameDefinition = gameEngine.getGameDefinition();
            console.log(`📋 Game: ${definition.meta.name}`);
            console.log(`🎯 Target age: ${definition.meta.targetAge}`);
            console.log(`👥 Player count: ${definition.meta.playerCount}`);
            
            // Show players with type safety
            const players: Player[] = gameEngine.getPlayers();
            console.log('👤 Players in room:');
            players.forEach((player: Player) => {
                console.log(`  - ${player.name} (ID: ${player.id}, Health: ${player.health})`);
            });
            
            // Generate and display cards
            const cardFactory: CardFactory = new CardFactory(definition);
            const cards: Card[] = cardFactory.generateCards();
            console.log(`🎴 Generated ${cards.length} cards:`);
            
            // Show sample cards with type checking
            this.displaySampleCards(cards.slice(0, 4));
            
            console.log(`✅ Room ${roomId} is ready with ${players.length} players!`);
            return roomId;
            
        } catch (error) {
            console.error(`❌ Failed to create game room ${roomId}:`, error);
            throw error;
        }
    }

    private displaySampleCards(cards: Card[]): void {
        cards.forEach((card: Card) => {
            const damage = card.getIntProperty('damage');
            const element = card.getStringProperty('element');
            const shield = card.getIntProperty('shield');
            const effect = card.getStringProperty('effect');
            const power = card.getIntProperty('power');
            
            let description = '';
            if (damage !== null && element !== null) {
                description = `${element} attack (${damage} damage)`;
            } else if (shield !== null) {
                description = `defense (${shield} shield)`;
            } else if (effect !== null && power !== null) {
                description = `${effect} spell (power ${power})`;
            } else {
                description = card.type;
            }
            
            console.log(`  - ${card.id}: ${description}`);
        });
    }

    getGameRoom(roomId: string): GameRoom | undefined {
        return this.games.get(roomId);
    }

    getAllRooms(): GameRoom[] {
        return Array.from(this.games.values());
    }

    private demonstrateGameManagement(): void {
        console.log('🎯 Demonstrating game management features...');
        
        const allRooms: GameRoom[] = this.getAllRooms();
        console.log(`🎮 Total active games: ${allRooms.length}`);
        
        allRooms.forEach((room: GameRoom) => {
            console.log(`📊 Room ${room.id}: ${room.players.length} players, created at ${room.createdAt.toISOString()}`);
            
            const players: Player[] = room.engine.getPlayers();
            console.log('  Current state:');
            players.forEach((player: Player) => {
                console.log(`    - ${player.name}: ${player.health} HP, ${player.hand.length} cards`);
            });
        });
        
        console.log('🏆 TypeScript server demo completed successfully!');
        console.log('💡 This demo showcases full TypeScript type safety with Catenin library');
    }

    // Utility method to demonstrate TypeScript generics
    getGamesByPlayerCount<T extends number>(playerCount: T): GameRoom[] {
        return this.getAllRooms().filter(room => room.players.length === playerCount);
    }
}

// Start the server
const server = new TypeScriptGameServer();
server.start();

// Export for potential module usage
export { TypeScriptGameServer };
export type { GameRoom };