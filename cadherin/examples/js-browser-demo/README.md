# JS Browser Demo

This example demonstrates how to use the Cadherin game engine in a web browser application.

## Features

- Web-based card game UI
- DOM manipulation and event handling
- TypeScript definition usage
- Browser local storage integration

## Running the Demo

```bash
# From the root project directory
./gradlew :cadherin:examples:js-browser-demo:jsBrowserRun

# Or build and open manually
./gradlew :cadherin:examples:js-browser-demo:jsBrowserDistribution
# Then open: cadherin/examples/js-browser-demo/build/distributions/index.html
```

## How it Works

The demo creates a simple web interface that loads the Cadherin game engine as a JavaScript module and demonstrates basic game functionality in the browser.

## Dependencies

This example uses:
- `implementation(project(":cadherin"))` for development
- For external projects: `implementation("org.junction.cadherin:cadherin:1.0.0")`

## Development

The Kotlin/JS code compiles to JavaScript modules that can be used in any web application. TypeScript definitions are automatically generated for better IDE support.