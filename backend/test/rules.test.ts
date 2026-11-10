import { assertFails, initializeTestEnvironment, type RulesTestEnvironment } from "@firebase/rules-unit-testing";
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

  it("nega leitura e escrita mesmo logado, inclusive no próprio perfil", async () => {
    const db = ambiente.authenticatedContext("uid-1").firestore();
    await assertFails(getDoc(doc(db, "users/uid-1")));
    await assertFails(setDoc(doc(db, "users/uid-1"), { pontos: 100 }));
  });
});
