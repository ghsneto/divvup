import { assertFails, assertSucceeds, initializeTestEnvironment, type RulesTestEnvironment } from "@firebase/rules-unit-testing";
import { doc, getDoc, setDoc } from "firebase/firestore";
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { afterAll, beforeAll, describe, it } from "vitest";
import { FIRESTORE, PROJETO } from "./ambiente.js";

describe("Security Rules (padrão negar)", () => {
  let ambiente: RulesTestEnvironment;

  beforeAll(async () => {
    const [host, porta] = FIRESTORE.split(":");
    ambiente = await initializeTestEnvironment({
      projectId: PROJETO,
      firestore: {
        rules: readFileSync(join(dirname(fileURLToPath(import.meta.url)), "../../firebase/firestore.rules"), "utf8"),
        host: host ?? "127.0.0.1",
        port: Number(porta),
      },
    });
  });

  afterAll(() => ambiente.cleanup());

  it("nega leitura e escrita sem login", async () => {
    const db = ambiente.unauthenticatedContext().firestore();
    await assertFails(getDoc(doc(db, "houses/h1")));
    await assertFails(setDoc(doc(db, "houses/h1"), { nome: "x" }));
  });

  it("nega leitura e escrita de coleções não liberadas, mesmo logado", async () => {
    const db = ambiente.authenticatedContext("uid-1").firestore();
    await assertFails(getDoc(doc(db, "houses/h1")));
    await assertFails(setDoc(doc(db, "houses/h1"), { nome: "x" }));
  });
});

describe("Security Rules de users/{uid}", () => {
  let ambiente: RulesTestEnvironment;

  beforeAll(async () => {
    const [host, porta] = FIRESTORE.split(":");
    ambiente = await initializeTestEnvironment({
      projectId: PROJETO,
      firestore: {
        rules: readFileSync(join(dirname(fileURLToPath(import.meta.url)), "../../firebase/firestore.rules"), "utf8"),
        host: host ?? "127.0.0.1",
        port: Number(porta),
      },
    });
    await ambiente.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "users/uid-1"), { displayName: "Iane", totalPoints: 0 });
      await setDoc(doc(ctx.firestore(), "users/uid-2"), { displayName: "Bruno", totalPoints: 0 });
    });
  });

  afterAll(() => ambiente.cleanup());

  it("o próprio usuário lê o seu perfil", async () => {
    await assertSucceeds(getDoc(doc(ambiente.authenticatedContext("uid-1").firestore(), "users/uid-1")));
  });

  it("ninguém lê o perfil de outra pessoa nem sem login", async () => {
    await assertFails(getDoc(doc(ambiente.authenticatedContext("uid-1").firestore(), "users/uid-2")));
    await assertFails(getDoc(doc(ambiente.unauthenticatedContext().firestore(), "users/uid-1")));
  });

  it("o cliente não grava o próprio perfil (pontos são do backend)", async () => {
    const db = ambiente.authenticatedContext("uid-1").firestore();
    await assertFails(setDoc(doc(db, "users/uid-1"), { displayName: "Iane", totalPoints: 9999 }));
    await assertFails(setDoc(doc(db, "users/uid-3"), { displayName: "Novo", totalPoints: 0 }));
  });
});
