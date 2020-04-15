import Dexie from 'dexie';

const db = new Dexie('xyzspa');
db.version(1).stores({ savedState: '++id' });

export default db;