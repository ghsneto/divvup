# Regras de R8 do app. Bibliotecas trazem as próprias regras de consumidor.

# Rotas da navegação tipada (@Serializable): o Navigation lê nome e serializador por reflexão.
-keep @kotlinx.serialization.Serializable class br.app.divvup.** { *; }
-keepclassmembers class br.app.divvup.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
