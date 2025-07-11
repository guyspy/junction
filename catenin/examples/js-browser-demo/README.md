# JS Browser Demo

This example demonstrates how to use the Catenin game engine in a web browser application.

## Features

- **JavaScript Library Usage**: Shows how to use Catenin as a pure JavaScript library
- **ES6 Module Imports**: Uses standard `import` statements to load the library
- **Interactive YAML Editor**: Edit and test game definitions in the browser
- **Real-time Game Creation**: Create games using `GameEngine.fromYaml()`
- **TypeScript Support**: Auto-generated TypeScript definitions (.d.ts) included
- **Cross-platform Logic**: Uses the same game engine that runs on JVM servers

## Running the Demo

```bash
# From the monorepo root (/junction/)
# Build the library and prepare the demo
./gradlew :catenin:examples:js-browser-demo:serve

# Then open in browser:
# catenin/examples/js-browser-demo/index.html
```

**Note**: This demo references the JavaScript library files from the build directory. The HTML file imports directly from `../../build/dist/js/developmentLibrary/junction-catenin.mjs`.

## How it Works

This demo shows how to use Catenin as a **JavaScript library** in a regular HTML page:

1. **Builds** Catenin as a JavaScript ES6 module (`junction-catenin.mjs`)
2. **Imports** the library using standard ES6 module syntax
3. **Uses** the library API: `createGameEngineFromYaml()`, `GameDefinitionParser`, `CardFactory`
4. **Demonstrates** YAML parsing, game creation, and card generation
5. **Shows** how any web developer can integrate Catenin into their projects

**JavaScript Usage Example:**
```javascript
import { GameEngine, GameDefinitionParser, CardFactory, createGameEngineFromYaml } from '../../build/dist/js/developmentLibrary/junction-catenin.mjs';

const parser = new GameDefinitionParser();
const definition = parser.parseFromString(yamlContent);
const engine = createGameEngineFromYaml(yamlContent, ['Alice', 'Bob']);
const cardFactory = new CardFactory(definition);

// All methods are JavaScript-friendly by default, returning Arrays
const players = engine.getPlayers();  // Returns JavaScript Array
const cards = cardFactory.generateCards();  // Returns JavaScript Array
```

## Browser Compatibility

The demo works in modern browsers with:
- ES6 module support
- Local Storage API
- DOM manipulation capabilities

## Dependencies

This example uses:
- `implementation(project(":catenin"))` for monorepo development
- For external projects: `implementation("org.junction.catenin:catenin:1.0.0")`

## What You Get

When you build this demo, you get:
- **JavaScript Library**: `junction-catenin.mjs` - The main Catenin library
- **TypeScript Definitions**: `junction-catenin.d.ts` - For IDE support
- **Dependencies**: All required libraries (kaml, kotlinx-serialization, etc.)
- **HTML Demo**: Complete working example in `index.html`
- **Source Maps**: Full debugging support in browser DevTools

## External Usage

To use Catenin in your own projects:
1. **Build the library**: `./gradlew :catenin:jsBrowserDevelopmentLibraryDistribution`
2. **Copy files**: From `catenin/build/dist/js/developmentLibrary/`
3. **Import in HTML**: `import { createGameEngineFromYaml } from './junction-catenin.mjs'`

## Use Cases

This browser setup is ideal for:
- **Client-side Games**: Single-player or offline multiplayer games
- **Web Applications**: Integration with existing web apps
- **Prototyping**: Quick game concept validation
- **Educational Tools**: Interactive learning experiences
- **Progressive Web Apps**: Offline-capable game applications