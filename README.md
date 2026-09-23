# Gota — lembrete para beber água (Android)

App nativo em Kotlin + Jetpack Compose (Material 3), com tema claro e escuro.

## O que ele faz
- **Hoje**: copo animado que se enche conforme você bebe, botões rápidos (+seu copo, +150, +350…), valor personalizado e "desfazer".
- **Notificações** com botões "Bebi 250 ml" e "Lembrar em 15 min", direto da barra de notificações.
- **Histórico**: gráfico dos últimos 7 dias, média, metas batidas, sequência de dias e total da semana.
- **Ajustes**: intervalo (15 min a 3 h), horário de início e fim, meta diária (com cálculo pelo peso), tamanho do copo, pausa ao bater a meta, notificação de teste.
- Os lembretes continuam após reiniciar o celular (WorkManager). Cada vez que você registra água, a contagem até o próximo aviso recomeça.

## Como gerar o app
**Opção 1, Android Studio (recomendado):** abra a pasta `Gota`, espere o Gradle sincronizar e clique em ▶ Run com o celular conectado (depuração USB ativada). Para gerar um APK: *Build → Build App Bundle(s) / APK(s) → Build APK(s)*.

**Opção 2, sem instalar nada:** suba a pasta para um repositório no GitHub. O workflow em `.github/workflows/build-apk.yml` compila automaticamente; baixe o APK em *Actions → Gerar APK → Artifacts*.

**Opção 3, linha de comando:** com JDK 17 e Android SDK instalados, rode `./gradlew assembleRelease`. O APK fica em `app/build/outputs/apk/release/`.

Requisitos: Android 8.0 ou superior.

## Dicas
- No Android 13+ o app pede permissão de notificação na primeira abertura.
- Alguns fabricantes (Xiaomi, Samsung, Motorola…) atrasam tarefas em segundo plano. Se os avisos atrasarem, use o botão "Otimização de bateria" em Ajustes e marque o Gota como "Não otimizar".
- O APK de release é assinado com a chave de debug, bom para uso pessoal. Para publicar na Play Store, configure sua própria chave.
"# gota" 
