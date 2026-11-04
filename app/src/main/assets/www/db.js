/* Almacenamiento local con IndexedDB: zonas, clientes y entradas de historial. */
(function () {
  'use strict';

  const DB_NAME = 'mis-clientes';
  const DB_VERSION = 1;
  let dbPromise = null;

  function open() {
    if (dbPromise) return dbPromise;
    dbPromise = new Promise((resolve, reject) => {
      const req = indexedDB.open(DB_NAME, DB_VERSION);
      req.onupgradeneeded = () => {
        const db = req.result;
        if (!db.objectStoreNames.contains('zones')) {
          db.createObjectStore('zones', { keyPath: 'id' });
        }
        if (!db.objectStoreNames.contains('clients')) {
          const s = db.createObjectStore('clients', { keyPath: 'id' });
          s.createIndex('zoneId', 'zoneId');
        }
        if (!db.objectStoreNames.contains('entries')) {
          const s = db.createObjectStore('entries', { keyPath: 'id' });
          s.createIndex('clientId', 'clientId');
        }
      };
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
    return dbPromise;
  }

  function promisify(req) {
    return new Promise((resolve, reject) => {
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
  }

  async function store(name, mode) {
    const db = await open();
    return db.transaction(name, mode).objectStore(name);
  }

  function txDone(tx) {
    return new Promise((resolve, reject) => {
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
      tx.onabort = () => reject(tx.error);
    });
  }

  const DB = {
    uid() {
      return Date.now().toString(36) + Math.random().toString(36).slice(2, 8);
    },
    async all(name) {
      return promisify((await store(name, 'readonly')).getAll());
    },
    async get(name, id) {
      return promisify((await store(name, 'readonly')).get(id));
    },
    async put(name, value) {
      return promisify((await store(name, 'readwrite')).put(value));
    },
    async remove(name, id) {
      return promisify((await store(name, 'readwrite')).delete(id));
    },
    async byIndex(name, index, value) {
      return promisify((await store(name, 'readonly')).index(index).getAll(value));
    },
    /** Borra un cliente junto con todo su historial. */
    async removeClient(id) {
      const db = await open();
      const tx = db.transaction(['clients', 'entries'], 'readwrite');
      tx.objectStore('clients').delete(id);
      const idx = tx.objectStore('entries').index('clientId');
      const keys = await promisify(idx.getAllKeys(id));
      keys.forEach((k) => tx.objectStore('entries').delete(k));
      return txDone(tx);
    },
    /** Borra una zona; sus clientes quedan "Sin zona". */
    async removeZone(id) {
      const db = await open();
      const tx = db.transaction(['zones', 'clients'], 'readwrite');
      tx.objectStore('zones').delete(id);
      const clients = await promisify(tx.objectStore('clients').index('zoneId').getAll(id));
      clients.forEach((c) => {
        c.zoneId = '';
        tx.objectStore('clients').put(c);
      });
      return txDone(tx);
    },
    /** Inserta o actualiza muchos registros en una sola transacción. */
    async bulkPut(data) {
      const db = await open();
      const tx = db.transaction(['zones', 'clients', 'entries'], 'readwrite');
      (data.zones || []).forEach((z) => tx.objectStore('zones').put(z));
      (data.clients || []).forEach((c) => tx.objectStore('clients').put(c));
      (data.entries || []).forEach((e) => tx.objectStore('entries').put(e));
      return txDone(tx);
    },
  };

  window.DB = DB;
})();
