import {
  createIdentity,
  restoreIdentity,
  exportIdentity,
  type Identity,
} from "./signing";
import { hex, sha256 } from "./bytes";
function database(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open("gba-studio-web", 1);
    request.onupgradeneeded = () =>
      request.result.createObjectStore("identity");
    request.onsuccess = () => resolve(request.result);
    request.onerror = () =>
      reject(
        new Error(
          "Não foi possível guardar a chave. Permita o armazenamento local neste navegador.",
        ),
      );
  });
}
async function read(): Promise<Identity | undefined> {
  const db = await database();
  try {
    return await new Promise((resolve, reject) => {
      const tx = db.transaction("identity"),
        r = tx.objectStore("identity").get("current");
      r.onsuccess = () => resolve(r.result);
      r.onerror = () => reject(r.error);
    });
  } finally {
    db.close();
  }
}
async function save(identity: Identity, replace = false): Promise<Identity> {
  const db = await database();
  try {
    return await new Promise((resolve, reject) => {
      const tx = db.transaction("identity", "readwrite"),
        store = tx.objectStore("identity");
      let selected = identity;
      const request = store.get("current");
      request.onsuccess = () => {
        if (request.result && !replace) selected = request.result;
        else store.put(identity, "current");
      };
      tx.oncomplete = () => resolve(selected);
      tx.onerror = () =>
        reject(new Error("Não foi possível guardar a chave local."));
      tx.onabort = tx.onerror;
    });
  } finally {
    db.close();
  }
}
export async function getIdentity(): Promise<Identity> {
  return (await read()) ?? (await save(await createIdentity()));
}
export async function restoreBackup(text: string): Promise<Identity> {
  return save(await restoreIdentity(text), true);
}
export async function backup(): Promise<string> {
  return exportIdentity(await getIdentity());
}
export async function fingerprint(identity: Identity): Promise<string> {
  return hex(await sha256(identity.certificate))
    .match(/.{1,4}/g)!
    .slice(0, 4)
    .join(" ");
}
