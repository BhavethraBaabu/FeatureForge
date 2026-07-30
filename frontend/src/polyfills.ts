// sockjs-client expects a Node-style `global` object, which doesn't exist
// in the browser. This must load before zone.js and before any app code
// (which imports sockjs-client transitively via WebsocketService), or the
// whole bundle throws ReferenceError: global is not defined and the app
// never mounts.
(window as any).global = window;

import 'zone.js';
