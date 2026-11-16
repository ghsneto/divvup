# Release (Play Store)

O workflow `release.yml` gera o AAB do flavor `prod` com R8, assinado com a **chave de upload**
(a Play guarda a chave do app, "Assinatura de apps do Google Play"). Roda em Actions › Run
workflow ou ao criar uma tag `v*`; o `versionCode` é o número da execução.

Segredos no GitHub (Settings › Secrets and variables › Actions):

| Segredo | O que é |
|---|---|
| `GOOGLE_SERVICES_JSON_PROD` | já existe (ADR 0002) |
| `DIVVUP_KEYSTORE_BASE64` | keystore de upload em base64 (`base64 -w0 upload.jks`) |
| `DIVVUP_KEYSTORE_PASSWORD` | senha do keystore |
| `DIVVUP_KEY_ALIAS` | alias da chave (ex.: `upload`) |
| `DIVVUP_KEY_PASSWORD` | senha da chave |

Gerar a chave de upload no seu computador (guarde o arquivo e as senhas num lugar seguro):

```bash
keytool -genkeypair -v -keystore upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

Crashlytics e Analytics coletam só no flavor `prod`; no `dev` ficam desligados (projeto
fictício, ADR 0002). O mapeamento do R8 vai para o Crashlytics no build de release da CI.
