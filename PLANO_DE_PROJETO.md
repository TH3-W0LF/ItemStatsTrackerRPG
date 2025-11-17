# ItemStatsTracker - Plano de Projeto e Documentação Técnica

**Versão:** 4.0  
**Autor:** MestreBR  
**Co-Founder:** ShelbyKING_  
**Descrição:** Give your items a soul, tracks usage statistics and dynamically updates the item's lore, making it evolve alongside the player.  
**Última Atualização:** Hoje (Sessão Completa)

---

## 📋 Índice

1. [Visão Geral](#visão-geral)
2. [Funcionalidades Implementadas](#funcionalidades-implementadas)
3. [Sistema de Estatísticas](#sistema-de-estatísticas)
4. [Sistema de Encantamentos](#sistema-de-encantamentos)
5. [Sistema de Reincarnação](#sistema-de-reincarnação)
6. [Sistema de Resistências](#sistema-de-resistências)
7. [Sistema de Conjuntos de Armadura](#sistema-de-conjuntos-de-armadura)
8. [Sistema de Filtro de Rastreamento](#sistema-de-filtro-de-rastreamento)
9. [Sistema de Gemas](#sistema-de-gemas)
10. [Sistema de Acessórios](#sistema-de-acessórios)
11. [Integrações](#integrações)
12. [Arquivos de Configuração](#arquivos-de-configuração)
13. [Recursos Visuais](#recursos-visuais)
14. [Comandos](#comandos)
15. [Estrutura de Arquivos](#estrutura-de-arquivos)

---

## 🎯 Visão Geral

O **ItemStatsTracker** é um plugin avançado para Minecraft que transforma itens em entidades vivas, rastreando seu uso e evoluindo junto com o jogador. Cada item possui estatísticas únicas, encantamentos customizados, níveis de ascensão e bônus especiais.

### Características Principais

- ✅ Rastreamento detalhado de estatísticas de uso
- ✅ Sistema de reincarnação de itens (renomeado de "ascensão")
- ✅ Bônus de resistência baseados em reincarnação
- ✅ Sistema de conjuntos de armadura com bônus progressivos
- ✅ Sistema de filtro de rastreamento (apenas itens customizados)
- ✅ Sistema de gemas (slots e socketing)
- ✅ Sistema de acessórios (slots virtuais para anéis, colares, asas)
- ✅ Integração com AdvancedEnchantments (AE)
- ✅ Integração com BeaconPower (BP)
- ✅ Sistema visual avançado com cores temáticas
- ✅ Lore dinâmica e personalizável
- ✅ Estatísticas específicas por tipo de item
- ✅ Sistema de progresso para encantamentos vanilla e customizados
- ✅ Formatação consistente com colchetes
- ✅ HIDE_ATTRIBUTES configurável (ocultar atributos vanilla)
- ✅ Limites de encantamentos por categoria de item

---

## 🚀 Funcionalidades Implementadas

### 1. Sistema de Estatísticas

O plugin rastreia automaticamente várias estatísticas baseadas no tipo de item:

#### Estatísticas para Armas (Espadas, Machados, etc.)
- **Abates de Mobs** (`MOB_KILLS`)
- **Dano Causado (Total)** (`DAMAGE_DEALT`)
- **Dano em Mobs** (`DAMAGE_DEALT_MOBS`)
- **Dano em Mortos-Vivos** (`DAMAGE_DEALT_UNDEAD`)
- **Dano em Jogadores** (`DAMAGE_DEALT_PLAYER`)

#### Estatísticas para Armaduras
- **Dano Recebido** (`DAMAGE_TAKEN`)
- **Dano Recebido (Total)** (`DAMAGE_TAKEN_TOTAL`)
- **Dano por tipo de causa** (Fire, Fall, Explosion, etc.)

#### Estatísticas para Ferramentas
- **Blocos Excavados** (`BLOCKS_BROKEN`) - Pás
- **Lenha Coletada** (`WOOD_CHOPPED`) - Machados
- **Plantações Colhidas** (`FARM_HARVESTED`) - Enxadas
- **Terras Aradas** (`HOE_SOIL_TILLED`) - Enxadas
- **Minérios Quebrados** (`ORES_BROKEN`) - Picaretas
- **Blocos Totais** (`BLOCKS_TOTAL`) - Picaretas (soma de minérios + blocos)

#### Estatísticas para Arcos e Bestas
- **Alvos na Mira** (`BOW_ARROWS_SHOT`) - Flechas disparadas
- **Dano Total** (`DAMAGE_DEALT`) - Dano causado

#### Estatísticas para Tridentes
- **Lançamentos** (`TRIDENT_THROWN`) - Quantidade de vezes lançado
- **Dano com Tridente** (`TRIDENT_DAMAGE`) - Dano causado

#### Estatísticas para Maces
- **Altura Máxima** (`MACE_FALL_HEIGHT`) - Maior altura de queda alcançada (em blocos)
- **Maior Dano Aplicado** (`MACE_MAX_DAMAGE`) - Maior dano causado em um único golpe

#### Estatísticas para Elitros
- **Tempo de Voo** (`ELYTRA_FLIGHT_TIME`) - Tempo total voando (em segundos)

#### Estatísticas para Escudos
- **Dano Suportado** (`DAMAGE_BLOCKED`) - Dano bloqueado com o escudo

#### Estatísticas para Armaduras
- **Dano Recebido** (`DAMAGE_TAKEN`) - Dano recebido enquanto equipada

### 2. Sistema de Níveis de Reincarnação

Itens podem reincarnar através de níveis baseados em estatísticas acumuladas:

- **Níveis configuráveis** via `reincarnado.yml` (renomeado de `ascension.yml`)
- **Bônus de drop** progressivo por nível (multiplicador configurável)
- **Bônus de experiência** progressivo por nível (multiplicador configurável)
- **Bônus de resistência** por tipo de dano
- **Bônus de ataque** por tipo de alvo (PvP/PvE)
- **Bônus de ferramenta** (velocidade de mineração, chance de drop, XP)
- **Badge "ITEM MESTRE"** no nível 1000
- **Broadcast global** quando atinge nível mestre
- **Renomeação automática** de itens (opcional, via comando externo)

### 3. Sistema de Upgrades de Encantamentos

Encantamentos vanilla e customizados podem ser aprimorados através do uso:

- **Progresso visual** com barras de progresso
- **Níveis máximos configuráveis** por categoria de item
- **Critérios de upgrade** baseados em estatísticas
- **Exibição de progresso** em porcentagem
- **Barras de progresso para encantamentos padrão** - Mostra progresso mesmo quando o encantamento ainda não está no item
- **Suporte para encantamentos customizados** (AE, BP, etc.) - Formato: `PLUGIN:ENCHANT_NAME`
- **Aplicação automática** quando o progresso atinge 100%
- **Mensagens de upgrade** formatadas com MiniMessage (cores e gradientes)

---

## 🎨 Sistema de Encantamentos

### Encantamentos Vanilla

Todos os encantamentos vanilla são suportados com:
- **Nomes traduzidos** em português
- **Formatação visual** com gradientes elegantes
- **Cores neutras** (tons de cinza/branco)
- **Símbolo** `✦` para identificação visual
- **Níveis** com a mesma cor do encantamento (I, II, III... até 30k)

#### Encantamentos Configurados

**Armas:**
- Afiação (Sharpness/DAMAGE_ALL)
- Julgamento (Smite)
- Ruína dos Artrópodes (Bane of Arthropods)
- Aspecto Flamejante (Fire Aspect)
- Repulsão (Knockback)
- Pilhagem (Looting)
- Corte Varredor (Sweeping Edge)

**Armaduras:**
- Proteção (Protection)
- Proteção contra Explosões (Blast Protection)
- Proteção contra Fogo (Fire Protection)
- Proteção contra Projéteis (Projectile Protection)
- Peso Pena (Feather Falling)
- Respiração (Respiration)
- Afinidade Aquática (Aqua Affinity)
- Passos Profundos (Depth Strider)
- Velocidade das Almas (Soul Speed)
- Furtividade Rápida (Swift Sneak)

**Ferramentas:**
- Eficiência (Efficiency)
- Fortuna (Fortune)
- Toque Suave (Silk Touch)
- Inquebrável (Unbreaking)
- Remendo (Mending)

**Arcos:**
- Poder (Power)
- Impacto (Punch)
- Chama (Flame)
- Infinidade (Infinity)
- Múltiplos Tiros (Multishot)
- Perfuração (Piercing)
- Carga Rápida (Quick Charge)

**Tridentes:**
- Correnteza (Riptide)
- Condutividade (Channeling)
- Lealdade (Loyalty)
- Empalação (Impaling)

**Varas de Pesca:**
- Sorte do Mar (Luck of the Sea)
- Isca (Lure)

### Encantamentos do AdvancedEnchantments (AE)

Mais de **80 encantamentos AE** configurados com cores temáticas vibrantes:

#### Categorias de Encantamentos AE

**Vida e Regeneração:**
- Lifesteal (Vermelho/Laranja)
- Overload (Rosa/Laranja)
- Regeneration (Verde)

**Dano e Combate:**
- Berserk (Rosa/Vermelho)
- Sharpness (Vermelho claro)
- Smite (Laranja/Dourado)
- Frenzy (Rosa/Vermelho)
- Critical (Vermelho/Laranja)
- Brutal (Rosa/Vermelho)
- Revenge (Rosa/Vermelho)
- Strength (Rosa/Vermelho)

**Elementos:**
- Frost/Frozen (Azul gelo)
- Flame/Fire (Laranja/Vermelho)
- Thunder/Thunderlord/Lightning (Roxo)
- Poison/Toxic/Venom (Verde tóxico)
- Ice/Blizzard (Azul gelo)
- Plasma (Laranja/Vermelho)
- Explosive (Laranja/Vermelho)

**Utilidade e Sorte:**
- Luck/Lucky (Verde - gradiente de sorte)
- Epicness (Dourado)
- Haste (Dourado)
- Speed (Verde)
- Telepathy (Roxo)
- Allure (Rosa/Vermelho)
- Ambit (Rosa/Amarelo)
- Angelic (Amarelo claro)

**Defesa:**
- Armored (Cinza)
- Arrowbreak (Cinza claro)
- Reflect (Cinza claro)

**Especiais:**
- Abiding (Roxo)
- Aqua/Aquatic (Azul/Verde)
- Archer (Laranja/Amarelo)
- Blessed (Amarelo claro)
- Blinding (Vermelho escuro)
- Charged (Roxo)
- Curse (Vermelho escuro)
- Death (Cinza escuro)
- Devour (Vermelho escuro)
- Dragon (Laranja/Vermelho)
- Flash (Dourado)
- Glowing (Dourado)
- Healing (Verde)
- Inquisitive (Roxo)
- Levitate/Levitation (Azul/Verde)
- Metamorphosis (Rosa/Vermelho)
- Moon (Amarelo claro)
- Mystic (Roxo)
- Night (Cinza escuro)
- Omega (Dourado)
- Paralyze (Verde tóxico)
- Phantom (Cinza escuro)
- Plague (Verde tóxico)
- Prismarine (Azul)
- Rainbow (Rosa/Vermelho)
- Reaper (Cinza escuro)
- Shock (Roxo)
- Shriek (Vermelho escuro)
- Sky (Azul/Verde)
- Slow (Cinza escuro)
- Soul (Roxo)
- Spectral (Roxo)
- Spider (Verde tóxico)
- Spirit (Roxo)
- Star (Dourado)
- Storm (Roxo)
- Summon (Cinza escuro)
- Sun (Dourado)
- Vampire (Vermelho escuro)
- Void (Cinza escuro)
- Weakness (Cinza escuro)
- Wind (Azul/Verde)
- Wither (Cinza escuro)

### Formatação Visual

**Encantamentos Vanilla:**
- Formato: `<gradient:#cor1:#cor2>✦ Nome Nível</gradient>`
- Cores neutras elegantes
- Níveis com mesma cor do encantamento
- **Encantamentos de nível único (I) não exibem o nível** - Apenas o nome do encantamento é mostrado para encantamentos de nível 1 (ex: "Remendo", "Inquebrável" do AE)

**Encantamentos AE:**
- Formato: `<gradient:#cor1:#cor2>✦ Nome Nível</gradient>`
- Cores temáticas vibrantes
- Sem negrito (fonte normal)
- Níveis com mesma cor do encantamento

**Separação na Lore:**
- **Efeitos:** Encantamentos vanilla
- **Mágicos:** Encantamentos AE (header em negrito roxo)

---

## ⬆️ Sistema de Reincarnação

### Como Funciona

1. Itens acumulam estatísticas através do uso
2. Quando atingem critérios configurados, podem reincarnar
3. Cada nível de reincarnação desbloqueia:
   - Bônus de drop progressivo (multiplicador)
   - Bônus de experiência progressivo (multiplicador)
   - Bônus de resistência por tipo de dano
   - Bônus de ataque por tipo de alvo (PvP/PvE)
   - Bônus de ferramenta (velocidade, drop, XP)
   - Visual atualizado na lore

### Configuração

Arquivo: `reincarnado.yml` (renomeado de `ascension.yml`)

```yaml
reincarnado-criteria:
  default:
    - stat-type: "BLOCKS_BROKEN"
      required-value: 1000
      display-name-key: "stats.blocks_broken"
```

### Configuração de Bônus

Arquivo: `config.yml`

```yaml
reincarnado:
  bonus-drop-percentage-per-level: 0.005  # 0.5% por nível
  bonus-exp-percentage-per-level: 0.01   # 1% por nível
  max-level: 100
  rename:
    enabled: true
    use-command: true
    command-template: "itemeditar renomear {player} {new_name}"
    name-template: "<gray><base_name> <yellow>L<level>"
```

### Efeitos por Nível

Arquivo: `level_effects.yml`

```yaml
LEVEL_EFFECTS:
  1000:
    resistance_bonuses:
      PROJECTILE: 5.0
      FALL: 5.0
      FIRE: 5.0
      LAVA: 5.0
      MAGIC: 5.0
      ENTITY_ATTACK: 5.0
      ENTITY_EXPLOSION: 5.0
    attack_bonuses:
      PLAYER: 10.0
      MOB: 10.0
      UNDEAD: 10.0
    tool_bonuses:
      MINING_SPEED: 10.0
      DROP_CHANCE: 10.0
      EXP_BONUS: 10.0
```

### Nível Mestre (1000)

Quando um item atinge o nível 1000:
- **Badge especial** na lore: "ITEM MESTRE"
- **Broadcast global** personalizado
- **Comandos extras** executáveis (configuráveis)

---

## 🛡️ Sistema de Resistências

### Funcionamento

As resistências são aplicadas quando o jogador recebe dano:

1. **Coleta** todas as peças de armadura equipadas
2. **Soma** todas as resistências do mesmo tipo de dano
3. **Aplica** a redução de dano (cap de 95%)
4. **Exibe** na lore com cores temáticas

### Cores Temáticas

Sistema automático de cores baseado no tipo de dano:

| Tipo de Dano | Cores | Gradiente |
|-------------|-------|-----------|
| Fogo/Lava | Vermelho/Laranja | `#ff4d4d:#ff8c42` |
| Água/Gelo | Azul claro/Escuro | `#4facfe:#00f2fe` |
| Explosão | Roxo | `#9d50bb:#6e48aa` |
| Queda | Roxo azulado | `#667eea:#764ba2` |
| Projéteis | Laranja/Amarelo | `#f09819:#edde5d` |
| Magia | Verde tóxico | `#7bff00:#a8ff42` |
| Ataques Entidades (PvE) | Vermelho claro | `#ff6b6b:#ff8787` |
| Ataques Jogadores (PvP) | Vermelho/Laranja | `#ff4d4d:#ff8c42` |
| Sufocação | Cinza escuro | `#4a4a4a:#6a6a6a` |
| Raio | Dourado | `#ffd700:#ffed4e` |
| Padrão | Cinza elegante | `#c0c0c0:#e0e0e0` |

### Formatação de Exibição

**Resistências:**
- Formato: `Resistência a [Tipo]: +X.X%`
- PvE/PvP: `Ataque [PvE]` e `Ataque [PvP]` (usando colchetes)
- Todas as referências usam colchetes `[]` em vez de parênteses `()`

### Correção de Bug

**Problema anterior:** Resistências eram divididas por 4, reduzindo drasticamente o efeito.

**Solução:** Agora soma todas as resistências de todas as peças e aplica diretamente.

**Exemplo:**
- 4 peças com 15% cada = 60% de redução total (antes: 15%)

---

## 👕 Sistema de Conjuntos de Armadura

### Funcionamento

Quando um jogador equipa múltiplas peças do mesmo conjunto:

- **3 peças:** Ativa bônus parcial
- **4 peças:** Ativa bônus completo

### Bônus Configuráveis

**Atributos:**
- Vida máxima
- Velocidade de movimento
- Dano de ataque
- Resistência a knockback
- E mais...

**Efeitos de Poção:**
- Regeneração
- Força
- Velocidade
- Visão noturna
- E mais...

### Conjuntos Configurados

**Conjunto de Diamante:**
- 3 peças: +10% Resistência a Repulsão
- 4 peças: +20% Velocidade

**Conjunto de Netherite:**
- 3 peças: +1 Dano
- 4 peças: +2 Corações + Visão Noturna

### Mensagens Visuais

- **Ativação:** Gradiente verde com símbolo ▲
- **Desativação:** Gradiente vermelho com símbolo ▼
- **Estágio:** Mostra "3 Peças" ou "Conjunto Completo"

---

## 🔍 Sistema de Filtro de Rastreamento

### Funcionamento

O plugin pode ser configurado para rastrear apenas itens customizados (ex: ItemsAdder), ignorando itens vanilla:

- **Filtro configurável** via `config.yml`
- **Verificação de tags NBT/PDC** específicas
- **Ignora itens vanilla** quando o filtro está ativo
- **Remove automaticamente** dados de plugin de itens não rastreáveis

### Configuração

Arquivo: `config.yml`

```yaml
tracking-filter:
  enable: true
  required-tags:
    - "itemsadder:id"
    # - "outra_chave:id"
```

### Comportamento

- **Quando `enable: false`**: Rastreia todos os itens (comportamento padrão)
- **Quando `enable: true`**: Apenas itens com pelo menos uma das tags `required-tags` são rastreados
- **Itens não rastreáveis**: Não recebem dono, não atualizam lore, não acumulam estatísticas

## 💎 Sistema de Gemas

### Funcionamento

Itens rastreáveis podem ter slots de gemas onde jogadores podem "socar" gemas customizadas para adicionar bônus:

- **Slots configuráveis** por item (via comando admin)
- **Gemas definidas** em `gemas.yml`
- **Socketing via drag-and-drop** (arrastar e soltar)
- **Bônus aplicados** automaticamente aos stats do jogador
- **Armazenamento no PDC** do item

### Configuração

Arquivo: `gemas.yml`

```yaml
gemas:
  itemsadder:gema_forca_t1:
    display-name: "<red>Gema de Força (T1)</red>"
    lore-line: "§c+5 Dano de Ataque"
    stats:
      ATTACK_DAMAGE: 5.0
  itemsadder:gema_vida_t2:
    display-name: "<green>Gema de Vida (T2)</green>"
    lore-line: "§a+10 Vida Máxima"
    stats:
      MAX_HEALTH: 10.0
```

### Comandos Admin

- `/ist gema setslots <numero>` - Define o número de slots de gema do item na mão
- `/ist gema clear` - Remove todas as gemas do item na mão

### Armazenamento

- **Total de slots**: `istats:gema_slots_total` (Integer)
- **Gemas socadas**: `istats:gema_socket_N` (String - ID da gema ou "EMPTY")

## 👑 Sistema de Acessórios

### Funcionamento

Sistema de slots virtuais para equipar itens especiais que não têm slots no inventário vanilla:

- **GUI customizada** (`/acessorios`)
- **Slots configuráveis** (anéis, colares, asas, etc.)
- **Armazenamento no PDC do jogador** (não no item)
- **Bônus aplicados** automaticamente aos stats do jogador
- **Persistência** entre logins

### Configuração

Arquivo: `acessorios.yml`

```yaml
gui:
  title: "Seus Acessórios"
  rows: 3
  slots:
    10:
      type: "ANEL"
      placeholder-item: "GRAY_STAINED_GLASS_PANE"
      placeholder-name: "§7Slot de Anel 1"
    12:
      type: "COLAR"
      placeholder-item: "GRAY_STAINED_GLASS_PANE"
      placeholder-name: "§7Slot de Colar"
    14:
      type: "ASA"
      placeholder-item: "GRAY_STAINED_GLASS_PANE"
      placeholder-name: "§7Slot de Asa"

item_types:
  ANEL:
    - "itemsadder:anel_de_forca"
    - "itemsadder:anel_de_vida"
  COLAR:
    - "itemsadder:colar_de_agilidade"
  ASA:
    - "itemsadder:asas_angelicais"
```

### Comando

- `/acessorios` (aliases: `/acc`, `/equipamentos`) - Abre o menu de acessórios

### Armazenamento

- **Chave PDC**: `istats:acessorio_[TIPO]` (String - ItemStack serializado em Base64)
- **Tipos**: ANEL, COLAR, ASA, etc. (configuráveis)

### Funcionalidades

- **Equipar**: Arrastar item válido para o slot
- **Desequipar**: Clicar no item equipado
- **Validação**: Apenas itens configurados em `item_types` podem ser equipados
- **Sons**: Feedback sonoro ao equipar/desequipar

---

## 🔌 Integrações

### AdvancedEnchantments (AE)

**Funcionalidades:**
- Aplicação automática de encantamentos AE via comando
- Detecção e remoção de duplicatas na lore
- Tab-completion dinâmico para encantamentos disponíveis
- Resolução automática de níveis máximos
- Formatação visual temática

**Comando:**
```
/ist addeffect AE <encantamento> [nível]
```

**Exemplo:**
```
/ist addeffect AE LIFESTEAL 5
/ist addeffect AE LUCKY
```

### BeaconPower (BP)

**Funcionalidades:**
- Aplicação de efeitos de poção via beacon
- Suporte para múltiplos efeitos

**Comando:**
```
/ist addeffect BP POTION_EFFECT REGENERATION [nível]
```

---

## 📁 Arquivos de Configuração

### 1. `config.yml`

Configurações principais:
- Estatísticas habilitadas/desabilitadas
- Sistema de reincarnação
- Filtro de rastreamento
- HIDE_ATTRIBUTES (ocultar atributos vanilla)
- Limites de encantamentos por categoria
- Conjuntos de armadura
- Upgrades de encantamentos
- Exibição de informações na lore

### 2. `messages.yml`

Todas as mensagens do plugin:
- Headers de seções
- Mensagens de comandos
- Formatação de lore
- Mensagens de erro/sucesso
- Mensagens de reincarnação (renomeado de "ascensão")

### 3. `enchantments.yml`

Formatação de encantamentos:
- Encantamentos vanilla (ENCHANTMENTS)
- Encantamentos customizados (CUSTOM_EFFECTS)
- Cores, gradientes e símbolos
- Nomes de exibição traduzidos
- Formato de lore para cada encantamento

### 4. `level_effects.yml`

Efeitos por nível de reincarnação:
- Bônus de drop
- Bônus de experiência
- Bônus de resistência
- Bônus de ataque (PvP/PvE)
- Bônus de ferramenta
- Efeitos de poção
- Atributos

### 5. `reincarnado.yml`

Critérios de reincarnação:
- Critérios por categoria de item
- Valores requeridos por nível
- Chaves de exibição para mensagens

### 6. `gemas.yml`

Definições de gemas:
- IDs de itens (ItemsAdder)
- Nomes de exibição
- Linhas de lore
- Bônus de stats

### 7. `acessorios.yml`

Configuração do sistema de acessórios:
- Layout da GUI
- Slots e tipos
- Itens aceitos por tipo
- Placeholders visuais

### 8. `itemmestre.yml`

Configurações do nível mestre:
- Mensagem de broadcast
- Comandos extras executáveis
- Placeholders: {player}, {item}

### 6. `guia.yml`

Documentação para administradores:
- Guia de atributos
- Guia de efeitos de poção
- **Guia de cores temáticas** (novo)
- Exemplos e notas

---

## 🎨 Recursos Visuais

### Sistema de Cores

**Gradientes MiniMessage:**
- Suporte completo a gradientes
- Cores hexadecimais
- Cores nomeadas do Minecraft

**Formatação:**
- Negrito (`<bold>`)
- Itálico (`<italic>`)
- Cores sólidas
- Gradientes

### Seções da Lore

1. **Nome do Item** (Azul claro)
2. **Estatísticas** (Dourado header, Branco conteúdo) - Específicas por tipo de item
3. **Efeitos** (Dourado header) - Encantamentos vanilla
4. **Mágicos** (Roxo gradiente, negrito header) - Encantamentos AE
5. **Bônus de Resistência** (Dourado header, cores temáticas) - Para armaduras
6. **Bônus de Ataque** (Dourado header, cores temáticas) - Para armas
7. **Bônus de Ferramenta** (Dourado header, cores temáticas) - Para ferramentas
8. **Dono** (Cinza)
9. **Upgrades de Encantamento** (Dourado header, barras de progresso) - Inclui progresso para encantamentos ainda não aplicados
10. **Na mão principal** (Branco header, Verde stats) - Para armas e ferramentas
11. **No peito/Na cabeça/etc** (Branco header, Azul claro stats) - Para armaduras

### Barras de Progresso

- **10-15 segmentos** visuais
- **Verde** para progresso
- **Cinza** para vazio
- **Porcentagem** ao lado
- **Nome formatado** com mesma cor do encantamento

---

## ⌨️ Comandos

### `/ist` (Padronizado - antigo `/itemstats`)

Comando principal do plugin. Todos os comandos foram padronizados para `/ist`.

#### Subcomandos:

**`info [item]`**
- Mostra informações detalhadas do item
- Estatísticas, encantamentos, progresso de upgrades
- Progresso de reincarnação

**`reincarnado [silent]`** (renomeado de `ascend`)
- Reincarna o item para o próximo nível
- Requer que todos os critérios sejam atendidos
- `silent`: Não exibe mensagens de sucesso/erro

**`addeffect <plugin> <efeito> [nível]`** (renomeado de `addcustomeffect`)
- Adiciona efeito customizado (AE, BP, etc.)
- Validação automática
- Remoção de duplicatas
- Suporte para encantamentos customizados

**`removecustomeffect <efeito>`**
- Remove efeito customizado do item

**`giverr <nível> [jogador]`** (renomeado de `giveascension`)
- Define o nível de reincarnação do item
- Se `jogador` não for especificado, usa o executor

**`gema setslots <numero>`**
- Define o número de slots de gema do item na mão (admin)

**`gema clear`**
- Remove todas as gemas do item na mão (admin)

**`cleardono` ou `clearowner`**
- Remove o dono do item na mão
- Atualiza a lore imediatamente

**`set <estatistica> <valor>`**
- Define o valor de uma estatística (admin)

**`add <estatistica> <valor>`**
- Adiciona valor a uma estatística (admin)

**`reload`**
- Recarrega todas as configurações do plugin (admin)

**`setarrow <quantidade>`**
- Define a quantidade de flechas do item (admin)

### `/acessorios` (aliases: `/acc`, `/equipamentos`)

Abre o menu de acessórios para equipar anéis, colares, asas, etc.

### Permissões

- `itemstatstracker.use` - Uso básico
- `itemstatstracker.admin` - Comandos administrativos
- `itemstatstracker.acessorios` - Acesso ao menu de acessórios (padrão: true)

---

## 📂 Estrutura de Arquivos

```
ItemStatsTracker/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/drakkar/itemstatstracker/
│   │   │       ├── ItemStatsTracker.java (Main class)
│   │   │       ├── StatManager.java (Gerenciamento de stats, reincarnação, filtro)
│   │   │       ├── LoreManager.java (Gerenciamento de lore)
│   │   │       ├── StatListeners.java (Event handlers)
│   │   │       ├── StatCommands.java (Comandos)
│   │   │       ├── LanguageManager.java (Mensagens)
│   │   │       ├── GemaManager.java (Gerenciamento de gemas)
│   │   │       ├── GemaListener.java (Listener para socketing)
│   │   │       ├── AcessorioManager.java (Gerenciamento de acessórios)
│   │   │       ├── AcessorioGUI.java (Interface de acessórios)
│   │   │       ├── AcessorioListener.java (Listener da GUI)
│   │   │       ├── AcessorioCommand.java (Comando /acessorios)
│   │   │       └── utility/
│   │   │           └── AdvancedEnchantmentsBridge.java
│   │   └── resources/
│   │       ├── plugin.yml
│   │       ├── config.yml
│   │       ├── messages.yml
│   │       ├── enchantments.yml
│   │       ├── level_effects.yml
│   │       ├── reincarnado.yml (renomeado de ascension.yml)
│   │       ├── gemas.yml (novo)
│   │       ├── acessorios.yml (novo)
│   │       ├── itemmestre.yml
│   │       └── guia.yml
├── pom.xml
└── PLANO_DE_PROJETO.md (este arquivo)
```

---

## 🔧 Tecnologias e Dependências

### APIs Utilizadas

- **Bukkit/Spigot API 1.20**
- **Adventure API** (MiniMessage para formatação)
- **Persistent Data Container** (PDC) para armazenamento

### Soft Dependencies

- **PlaceholderAPI** (suporte a placeholders)
- **AdvancedEnchantments** (encantamentos customizados)
- **BeaconPower** (efeitos de beacon)

---

## 🐛 Correções e Melhorias Implementadas

### Versão 4.0 (Hoje)

1. ✅ **Renomeação completa de "Ascensão" para "Reincarnação"**
   - Todos os comandos, mensagens, arquivos e referências atualizados
   - `ascension.yml` → `reincarnado.yml`
   - `/ist ascend` → `/ist reincarnado`
   - Sistema de bônus mantido com nova nomenclatura

2. ✅ **Sistema de Filtro de Rastreamento**
   - Filtro configurável para rastrear apenas itens customizados
   - Verificação de tags NBT/PDC (ex: `itemsadder:id`)
   - Remoção automática de dados de plugin em itens não rastreáveis
   - Prevenção de atribuição de dono em itens vanilla

3. ✅ **Sistema de Gemas**
   - Slots de gemas configuráveis por item
   - Socketing via drag-and-drop
   - Bônus de stats aplicados automaticamente
   - Configuração via `gemas.yml`
   - Comandos admin: `/ist gema setslots`, `/ist gema clear`

4. ✅ **Sistema de Acessórios**
   - GUI customizada (`/acessorios`)
   - Slots virtuais para anéis, colares, asas
   - Armazenamento no PDC do jogador
   - Bônus aplicados automaticamente
   - Configuração via `acessorios.yml`

5. ✅ **Padronização de Comandos**
   - `/itemstats` → `/ist` (comando principal)
   - `/ist giveascension` → `/ist giverr`
   - `/ist addcustomeffect` → `/ist addeffect`
   - Remoção de aliases antigos

6. ✅ **Sistema HIDE_ATTRIBUTES**
   - Opção configurável para ocultar atributos vanilla (armadura, resistência, etc.)
   - Controle via `config.yml` → `hide-attributes.enabled`

7. ✅ **Limites de Encantamentos por Categoria**
   - Sistema expandido para categorias de itens (ex: `SWORDS`, `ARMOR_PIECES`)
   - Prioridade: categoria > material específico
   - Configuração via `config.yml` → `enchantment-limits.category-limits`

8. ✅ **Upgrade de Encantamentos Customizados**
   - Suporte para encantamentos AE, BP, etc.
   - Formato: `PLUGIN:ENCHANT_NAME` (ex: `AE:HASTEN`)
   - Aplicação automática quando progresso atinge 100%
   - Mensagens de upgrade formatadas com MiniMessage

9. ✅ **Correção de Bônus de Resistência em Armaduras**
   - Bônus agora são salvos corretamente no PDC
   - Aplicação imediata do meta após salvar bônus
   - Exibição correta na lore para armaduras com reincarnação

10. ✅ **Comando cleardono**
    - Remove o dono do item imediatamente
    - Atualiza a lore visualmente
    - Funciona em qualquer slot (mão, off-hand, armadura)

11. ✅ **Correção de Bug: Tridentes Duplicados**
    - Prevenção de duplicação ao lançar tridentes
    - Verificação correta do item na mão após lançamento

12. ✅ **Rastreamento de Elytra**
    - Estatística de tempo de voo implementada
    - Rastreamento via `PlayerMoveEvent` monitorando `isGliding()`
    - Exibição em segundos na lore

13. ✅ **Encantamentos Padrão para Todos os Tipos de Item**
    - Configurações padrão adicionadas para: AXES, SHOVELS, HOES, BOWS, TRIDENT, CHESTPLATE (Elytra), SHIELD
    - Exemplo: Elytra ganha Unbreaking baseado em tempo de voo

14. ✅ **Correção: DURABILITY → UNBREAKING**
    - Nome correto do encantamento vanilla

15. ✅ **Melhorias na Exibição de Ataque**
    - Separação clara entre PvP e PvE
    - PvP aparece primeiro na lore
    - Formatação com colchetes `[PvP]` e `[PvE]`

16. ✅ **Refatoração do StatManager**
    - Método `atualizarStats(Player)` implementado
    - Calcula e aplica bônus de: armaduras, itens na mão, gemas, acessórios
    - Chamado automaticamente em eventos relevantes

17. ✅ **Mensagens de Upgrade Melhoradas**
    - Formatação com MiniMessage
    - Cores e gradientes consistentes com a lore do item
    - Nome do encantamento formatado corretamente

### Versão 3.1

1. ✅ **Formatação de PvE/PvP** - Troca de parênteses para colchetes: `(PvE)` → `[PvE]`, `(PvP)` → `[PvP]`
2. ✅ **Formatação geral** - Todos os parênteses em lores trocados para colchetes `()` → `[]`
3. ✅ **Encantamentos de nível único** - Remoção do "I" romano para encantamentos de nível 1
4. ✅ **Renomeação de estatísticas**:
   - Pás: "Blocos Quebrados" → "Blocos Excavados"
   - Machados: "Madeiras Quebradas" → "Lenha Coletada"
5. ✅ **Novas estatísticas específicas por tipo de item**:
   - **Elitros**: Tempo de Voo (exibido em segundos)
   - **Armaduras**: Dano Recebido
   - **Escudos**: Dano Suportado
   - **Arcos/Bestas**: Alvos na Mira + Dano Total
   - **Tridentes**: Lançamentos + Dano com Tridente
   - **Maces**: Altura Máxima (em blocos) + Maior Dano Aplicado
   - **Enxadas**: Plantações Colhidas + Terras Aradas
6. ✅ **Sistema de progresso expandido** - Barras de progresso para encantamentos vanilla padrão mesmo quando ainda não estão no item
7. ✅ **Formatação especial de estatísticas**:
   - Tempo de voo: exibido em segundos (ex: "15.5s")
   - Altura de queda: exibida em blocos (ex: "10 blocos")
8. ✅ **Listeners implementados** para rastreamento de novas estatísticas:
   - Rastreamento de tridentes lançados e dano causado
   - Rastreamento de dano bloqueado por escudos
   - Rastreamento de altura de queda para maces
   - Rastreamento de maior dano aplicado para maces
   - Rastreamento de terras aradas para enxadas

### Versão 3.0

1. ✅ **Sistema de cores temáticas** para resistências
2. ✅ **Correção de bug** de resistências (soma correta)
3. ✅ **Separação visual** entre encantamentos vanilla e AE
4. ✅ **Remoção de duplicatas** na lore
5. ✅ **Tradução completa** de encantamentos vanilla
6. ✅ **Níveis com mesma cor** dos encantamentos
7. ✅ **Formatação visual** melhorada em todas as seções
8. ✅ **Sistema de filtragem** inteligente de lore
9. ✅ **80+ encantamentos AE** configurados
10. ✅ **Guia de cores** documentado

### Melhorias de Performance

- Rate limiting para atualizações de lore
- Cache de configurações
- Processamento otimizado de eventos

---

## 📝 Notas Técnicas

### Armazenamento de Dados

Todos os dados são armazenados no **Persistent Data Container (PDC)** do item ou do jogador:

**PDC do Item:**
- Estatísticas: `istats:STAT_TYPE`
- Efeitos customizados: `ieffects:EFFECT_KEY`
- Bônus de resistência: `resistance_bonus` (TAG_CONTAINER_ARRAY)
- Bônus de ataque: `attack_bonus` (TAG_CONTAINER_ARRAY)
- Bônus de ferramenta: `tool_bonus` (TAG_CONTAINER_ARRAY)
- Nível de reincarnação: `reincarnado_level` (renomeado de `ascension_level`)
- Dono original: `original_owner`
- Progresso de upgrades: `enchantment_upgrade_progress`
- Slots de gemas: `gema_slots_total`, `gema_socket_N`

**PDC do Jogador:**
- Acessórios equipados: `acessorio_[TIPO]` (ItemStack serializado em Base64)

### Sistema de Markers

Marcadores na lore para identificação:
- `ISTATS:` - Linhas geradas pelo plugin
- `IEFFECTS:` - Efeitos customizados
- `IARMORSET:` - Informações de conjunto
- `IOwner:` - Informação do dono

### Filtragem de Lore

O sistema remove automaticamente:
- Duplicatas de encantamentos vanilla
- Duplicatas de encantamentos AE
- Lore gerada por outros plugins (quando conflita)
- Linhas vazias desnecessárias

---

## 🎯 Próximos Passos Sugeridos

### Melhorias Futuras

1. **Sistema de histórico** de estatísticas
2. **Gráficos de progresso** na lore
3. **Sistema de recompensas** por níveis
4. **API pública** para desenvolvedores
5. **Mais integrações** com plugins populares
6. **Sistema de rankings** de itens
7. **Exportação de dados** (JSON/CSV)

---

## 📞 Contato e Suporte

**Autor:** MestreBR  
**Co-Founder:** ShelbyKING_  
**Versão:** 3.1

---

---

## 📊 Estatísticas por Tipo de Item (Versão 3.1)

### Armas

**Espadas, Machados (como arma), Arcos, Bestas, Tridentes, Maces:**
- **Espadas/Machados/Maces**: Mobs Abatidos [PvE], Players Mortos [PvP], Dano Total
- **Arcos**: Alvos na Mira, Dano Total
- **Bestas**: Alvos na Mira, Dano Total
- **Tridentes**: Lançamentos, Dano com Tridente
- **Maces**: Altura Máxima (blocos), Maior Dano Aplicado

### Ferramentas

**Picaretas:**
- Minérios Quebrados
- Blocos Totais (soma de minérios + blocos)

**Machados:**
- Lenha Coletada

**Pás:**
- Blocos Excavados

**Enxadas:**
- Plantações Colhidas
- Terras Aradas

### Equipamentos Especiais

**Armaduras (Helmet, Chestplate, Leggings, Boots):**
- Dano Recebido

**Elitros:**
- Tempo de Voo (exibido em segundos, ex: "15.5s")

**Escudos:**
- Dano Suportado

---

## 🔄 Sistema de Progresso de Encantamentos (Versão 3.1)

### Funcionalidade Expandida

O sistema de progresso de encantamentos agora funciona para:

1. **Encantamentos já aplicados** - Mostra progresso para o próximo nível
2. **Encantamentos ainda não aplicados** - Mostra progresso para ganhar o primeiro nível (nível I)

### Como Funciona

- Todos os equipamentos que têm encantamentos configurados em `enchantment-upgrades` exibem barras de progresso
- Mesmo que o encantamento ainda não esteja no item, o jogador pode ver o progresso para ganhá-lo
- Quando o progresso atinge 100%, o encantamento é automaticamente aplicado ao item

### Exemplo de Configuração

```yaml
enchantment-upgrades:
  SWORDS:
    SHARPNESS:
      max-level: 5
      criteria:
        - stat-type: "DAMAGE_DEALT_MOB"
          required-value-per-level: 300
          display-name-key: "stats.damage_dealt_mob"
        - stat-type: "MOB_KILLS"
          required-value-per-level: 40
          display-name-key: "stats.mob_kills"
```

---

## 🎨 Melhorias de Formatação (Versão 3.1)

### Uso de Colchetes

Todas as referências em lores agora usam colchetes `[]` em vez de parênteses `()`:

- **Antes**: `Mobs Abatidos (PvE)`, `Players Mortos (PvP)`, `Dano Recebido (Total)`
- **Depois**: `Mobs Abatidos [PvE]`, `Players Mortos [PvP]`, `Dano Recebido [Total]`

### Encantamentos de Nível Único

Encantamentos que só têm nível 1 (como "Remendo", "Inquebrável" do AE) não exibem mais o "I" romano:

- **Antes**: `Remendo I`, `UNBREAK I`
- **Depois**: `Remendo`, `UNBREAK`

### Formatação de Estatísticas Especiais

- **Tempo de Voo**: Exibido em segundos com uma casa decimal (ex: "15.5s")
- **Altura de Queda**: Exibida em blocos (ex: "10 blocos")

---

---

## 📅 Histórico de Versões

### Versão 4.0 (Hoje)
- Sistema de Reincarnação (renomeado de Ascensão)
- Sistema de Filtro de Rastreamento
- Sistema de Gemas
- Sistema de Acessórios
- Padronização de comandos para `/ist`
- Suporte completo a encantamentos customizados no sistema de upgrade
- Correções de bugs e melhorias de performance

### Versão 3.1
- Formatação com colchetes
- Encantamentos de nível único
- Novas estatísticas específicas por tipo de item
- Sistema de progresso expandido

### Versão 3.0
- Sistema de cores temáticas
- Separação visual entre encantamentos vanilla e AE
- 80+ encantamentos AE configurados

---

**Documento gerado automaticamente - Última atualização:** Versão 4.0 (Hoje)  
**Confidencial - Acesso restrito ao MestreBR**

