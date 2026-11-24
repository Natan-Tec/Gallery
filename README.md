# Leve Galeria

Galeria Android leve, moderna e sem ligação com o ecossistema Samsung/Galaxy.
Kotlin + Jetpack Compose, sem internet, sem conta, sem anúncios.

## O que tem

- Aba **Fotos**: grade agrupada por dia, filtros (Tudo / Fotos / Vídeos / Favoritos)
- Aba **Álbuns**: todas as pastas com fotos/vídeos; segure uma pasta para ocultar
- **Ajustes → Pastas**: liga/desliga cada pasta; pasta desligada não mostra nenhum arquivo na galeria
- Visualizador em tela cheia: deslizar entre fotos, zoom com pinça e toque duplo, detalhes
- Favoritos, compartilhar, excluir (com confirmação do sistema), seleção múltipla (segure uma foto)
- Vídeos abrem no seu player padrão (mantém o app leve)
- Tema claro/escuro/sistema e tamanho da grade (2 a 6 colunas)
- Funciona no Android 8.0 ou mais novo

## Como gerar o APK (sem instalar nada no PC)

1. Crie um repositório no GitHub e envie todo o conteúdo desta pasta (incluindo `.github`).
2. Abra a aba **Actions** do repositório. O workflow **Build APK** roda sozinho a cada envio
   (ou clique em **Run workflow**).
3. Quando terminar, abra a execução e baixe o artefato **LeveGaleria-apk** (dentro vem o `.apk`).
4. Instale o APK no celular (permita "instalar apps desconhecidos" se o Android pedir).

O APK é assinado com a chave de debug do GitHub; cada build novo tem uma chave diferente,
então para atualizar é preciso desinstalar a versão anterior antes.

## Compilar localmente

Android Studio (Hedgehog ou mais novo) abre a pasta direto, ou use Gradle 8.9 + JDK 17:

```
gradle assembleRelease
```
