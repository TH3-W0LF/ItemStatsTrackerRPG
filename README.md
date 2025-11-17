# ItemStatsTracker

Plugin para Minecraft (Paper 1.21.4) que dá "alma" aos itens, rastreando estatísticas de uso e atualizando dinamicamente a lore dos itens, fazendo-os evoluir junto com o jogador.

## Funcionalidades

- ✅ **Rastreamento de Estatísticas**: Acompanha uso de itens (dano causado, blocos quebrados, kills, etc.)
- ✅ **Sistema de Reincarnação**: Itens evoluem com níveis de reincarnação que concedem bônus
- ✅ **Encantamentos Dinâmicos**: Encantamentos melhoram automaticamente baseado em estatísticas
- ✅ **Bônus de Ascensão**: Ataque, resistência e bônus de ferramentas baseados em níveis
- ✅ **Sistema de Gemas**: Gemas que podem ser aplicadas aos itens
- ✅ **Sistema de Acessórios**: Equipamentos especiais com slots
- ✅ **Conjuntos de Armadura**: Bônus especiais ao equipar conjuntos completos
- ✅ **Sistema de Itens Temporizados**: Itens que expiram após um determinado tempo
- ✅ **Lore Dinâmica**: Lore atualizada automaticamente com todas as informações do item

## Requisitos

- **Minecraft**: 1.21.4 (Paper)
- **Java**: 21+
- **Dependências**:
  - PlaceholderAPI (opcional, mas recomendado)
  - SQLite JDBC (incluído)
  - MySQL Connector (opcional, apenas se usar MySQL)

## Instalação

1. Baixe o arquivo `ItemStatsTracker-1.0-BETA.jar` da pasta `target/`
2. Coloque na pasta `plugins/` do seu servidor Paper
3. Reinicie o servidor
4. Configure os arquivos YAML gerados em `plugins/ItemStatsTracker/`

## Comandos

### Comandos Principais

- `/ist timer <segundos>` - Adiciona timer ao item na mão
- `/ist set <estatistica> <valor>` - Define valor de uma estatística
- `/ist add <estatistica> <valor>` - Adiciona valor a uma estatística
- `/ist info` - Mostra informações de progresso do item na mão
- `/ist reload` - Recarrega as configurações

### Comandos de Itens Temporizados

- `/timed give <player> <seconds> [material] [amount]` - Dá um item temporizado
- `/timed reload` - Recarrega configuração de itens temporizados

### Comandos de Acessórios

- `/acessorios` - Abre o menu de acessórios

## Permissões

- `itemstatstracker.use` - Usar comandos básicos (padrão: op)
- `itemstats.admin` - Comandos administrativos (padrão: op)
- `itemstatstracker.timed.admin` - Comandos de itens temporizados (padrão: op)
- `itemstatstracker.acessorios` - Usar acessórios (padrão: true)

## Configuração

O plugin gera vários arquivos de configuração:

- `config.yml` - Configurações gerais do plugin
- `level_effects.yml` - Efeitos por nível de reincarnação
- `enchantments.yml` - Configuração de encantamentos
- `reincarnado.yml` - Configuração de reincarnação
- `gemas.yml` - Configuração de gemas
- `acessorios.yml` - Configuração de acessórios
- `messages.yml` - Mensagens do plugin

## Sistema de Itens Temporizados

O plugin inclui um sistema completo de itens temporizados:

- Itens que expiram após X segundos
- Lore atualizada em tempo real mostrando tempo restante
- Expiração automática em inventários, containers e itens droppados
- Suporte a SQLite e MySQL para auditoria
- Eventos customizados para integração

Veja `TIMED_ITEMS_README.md` para mais detalhes.

## Desenvolvimento

### Compilar

```bash
mvn clean package
```

O JAR será gerado em `target/ItemStatsTracker-1.0-BETA.jar`

### Estrutura do Projeto

```
src/main/java/com/drakkar/itemstatstracker/
├── ItemStatsTracker.java          # Classe principal
├── StatManager.java                # Gerenciador de estatísticas
├── StatListeners.java              # Listeners de eventos
├── StatCommands.java               # Comandos do plugin
├── LoreManager.java                # Gerenciador de lore
├── LanguageManager.java            # Gerenciador de idiomas
├── GemaManager.java                # Sistema de gemas
├── AcessorioManager.java           # Sistema de acessórios
├── timed/                          # Sistema de itens temporizados
│   ├── TimedItemManager.java
│   ├── TimedItemExpirationTask.java
│   ├── TimedItemDBManager.java
│   └── ...
└── storage/                         # Gerenciamento de armazenamento
    └── StorageManager.java
```

## Licença

Este projeto é privado e proprietário.

## Autores

- **MestreBR** - Desenvolvimento principal
- **ShelbyKING_** - Contribuições

## Versão

**3.7** - Build 1.0-BETA

