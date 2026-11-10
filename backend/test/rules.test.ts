import { assertFails, assertSucceeds, initializeTestEnvironment, type RulesTestEnvironment } from "@firebase/rules-unit-testing";
import { doc, getDoc, setDoc, updateDoc } from "firebase/firestore";
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
    await assertFails(getDoc(doc(db, "reports/r1")));
    await assertFails(setDoc(doc(db, "reports/r1"), { nome: "x" }));
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

  it("o dono muda nome e avatar da galeria (B1/E3, R-32)", async () => {
    const db = ambiente.authenticatedContext("uid-1").firestore();
    await assertSucceeds(updateDoc(doc(db, "users/uid-1"), { displayName: "Iane S.", avatarId: "avataaars:divvup-07" }));
  });

  it("não aceita avatar fora da galeria, nome inválido, outros campos ou perfil alheio", async () => {
    const db = ambiente.authenticatedContext("uid-1").firestore();
    await assertFails(updateDoc(doc(db, "users/uid-1"), { displayName: "Iane", avatarId: "avataaars:divvup-25" }));
    await assertFails(updateDoc(doc(db, "users/uid-1"), { displayName: "", avatarId: "avataaars:divvup-01" }));
    await assertFails(updateDoc(doc(db, "users/uid-1"), { displayName: "x".repeat(25), avatarId: "avataaars:divvup-01" }));
    await assertFails(updateDoc(doc(db, "users/uid-1"), { displayName: "Iane", avatarId: "avataaars:divvup-01", totalPoints: 999 }));
    await assertFails(updateDoc(doc(db, "users/uid-2"), { displayName: "Hack", avatarId: "avataaars:divvup-01" }));
  });

  it("o cliente não grava o próprio perfil (pontos são do backend)", async () => {
    const db = ambiente.authenticatedContext("uid-1").firestore();
    await assertFails(setDoc(doc(db, "users/uid-1"), { displayName: "Iane", totalPoints: 9999 }));
    await assertFails(setDoc(doc(db, "users/uid-3"), { displayName: "Novo", totalPoints: 0 }));
  });
});

describe("Security Rules de houses/{casaId}", () => {
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
      const db = ctx.firestore();
      await setDoc(doc(db, "houses/h1"), { name: "Rep. Cajueiro", memberCount: 2 });
      await setDoc(doc(db, "houses/h1/members/ativo"), { role: "ADMIN", status: "ACTIVE" });
      await setDoc(doc(db, "houses/h1/members/saiu"), { role: "MEMBER", status: "LEFT" });
      await setDoc(doc(db, "houses/h1/tasks/t1"), { title: "Lavar a louça", points: 50 });
    });
  });

  afterAll(() => ambiente.cleanup());

  it("membro ativo lê a casa e o que tem dentro", async () => {
    const db = ambiente.authenticatedContext("ativo").firestore();
    await assertSucceeds(getDoc(doc(db, "houses/h1")));
    await assertSucceeds(getDoc(doc(db, "houses/h1/members/saiu")));
    await assertSucceeds(getDoc(doc(db, "houses/h1/tasks/t1")));
  });

  it("quem saiu, quem nunca entrou e quem não está logado não leem", async () => {
    for (const ctx of [ambiente.authenticatedContext("saiu"), ambiente.authenticatedContext("estranho"), ambiente.unauthenticatedContext()]) {
      const db = ctx.firestore();
      await assertFails(getDoc(doc(db, "houses/h1")));
      await assertFails(getDoc(doc(db, "houses/h1/tasks/t1")));
    }
  });

  it("nem membro ativo grava direto na casa (tudo passa pelo backend)", async () => {
    const db = ambiente.authenticatedContext("ativo").firestore();
    await assertFails(updateDoc(doc(db, "houses/h1"), { name: "Outra" }));
    await assertFails(setDoc(doc(db, "houses/h1/members/novo"), { role: "ADMIN", status: "ACTIVE" }));
    await assertFails(updateDoc(doc(db, "houses/h1/tasks/t1"), { points: 100 }));
  });
});
