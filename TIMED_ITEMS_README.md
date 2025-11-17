# Sistema de Itens Temporizados - ItemStatsTracker

Sistema completo para criar e gerenciar itens que expiram após um determinado tempo. Integrado ao plugin ItemStatsTracker.

## Funcionalidades

✅ **Gravação de expiração no ItemStack (PDC)** - Dados persistem mesmo após reinício do servidor  
✅ **Lore mostrando tempo restante** - Atualizada automaticamente a cada segundo  
✅ **Contagem regressiva em tempo real** - Funciona em inventários, ender chest e itens droppados  
✅ **Expiração automática** - Remove itens expirados em inventários, containers e no chão  
✅ **Marcação clara** - Diferenciados de itens normais via PDC tag  
✅ **Evento personalizado** - `TimedItemExpiredEvent` disparado quando item expira  
✅ **Persistência em banco** - SQLite (padrão) ou MySQL para auditoria  
✅ **Comandos administrativos** - Dar itens temporizados facilmente  
✅ **Configuração completa** - Intervalos de scan e opções de performance configuráveis  

## Instalação

O sistema já está integrado ao ItemStatsTracker. Certifique-se de que o plugin está ativado e configure no `config.yml`:

```yaml
timed-items:
  enabled: true
```

## Configuração

### config.yml

```yaml
timed-items:
  # Habilitar/desabilitar o sistema
  enabled: true
  
  # Configuração de banco de dados
  database:
    type: SQLITE  # SQLITE ou MYSQL
    sqlite-file: timed_items.db
    
    # Se usar MySQL:
    mysql:
      host: localhost
      port: 3306
      database: timeditems
      user: root
      password: password
  
  # Agendamento (performance)
  scheduling:
    update_interval_ticks: 20  # Atualização de inventários (20 = 1s)
    container_scan_interval_seconds: 60  # Scan de containers (60s)
  
  # Mensagens
  messages:
    give_msg: "§aVocê recebeu um item temporizado por %seconds% segundos."
    expired_msg: "§cUm item expirou e foi removido."
  
  # Lore
  lore:
    prefix: "§7[Expira em] "
    position: -1  # -1 = final da lore
  
  # Limites
  limits:
    max_duration_seconds: 86400  # 1 dia
```

## Comandos

### Dar Item Temporizado

```
/timed give <player> <seconds> [material] [amount]
```

**Exemplos:**
```
/timed give MestreBR 3600 DIAMOND 1
/timed give MestreBR 60 IRON_INGOT 64
/timed give MestreBR 86400 NETHERITE_SWORD 1
```

### Recarregar Configuração

```
/timed reload
```

## Permissões

- `itemstatstracker.timed.admin` - Acesso aos comandos de itens temporizados (padrão: op)

## Como Funciona

### Armazenamento

Os dados são salvos no **PersistentDataContainer (PDC)** do ItemStack:

- `timed_expire_at` (LONG) - Timestamp de expiração em milissegundos
- `timed_expire_id` (STRING) - UUID único do registro no banco
- `timed_tag` (BYTE) - Flag que identifica o item como temporizado

### Atualização

O sistema possui dois schedulers:

1. **Update Task** (1 segundo por padrão)
   - Atualiza inventários de jogadores online
   - Atualiza ender chests
   - Atualiza itens droppados no mundo
   - Atualiza a lore com tempo restante

2. **Container Scan Task** (60 segundos por padrão)
   - Escaneia containers carregados (baús, fornos, etc.)
   - Executado em thread assíncrona para não travar o servidor

### Expiração

Quando um item expira:
1. É removido do inventário/container/chão
2. O registro é deletado do banco de dados
3. O evento `TimedItemExpiredEvent` é disparado
4. Uma mensagem é enviada ao jogador (se aplicável)

## Banco de Dados

### SQLite (Padrão)

O arquivo `timed_items.db` é criado automaticamente na pasta do plugin.

### MySQL

Configure no `config.yml` e certifique-se de que o driver MySQL está disponível no servidor.

### Schema

```sql
CREATE TABLE IF NOT EXISTS timed_items (
  id VARCHAR(36) PRIMARY KEY,
  owner_uuid VARCHAR(36),
  material VARCHAR(64),
  amount INT,
  expire_at BIGINT,
  location TEXT,
  created_at BIGINT
);
```

## Evento Customizado

Você pode escutar o evento `TimedItemExpiredEvent` para executar ações quando um item expira:

```java
@EventHandler
public void onItemExpired(TimedItemExpiredEvent event) {
    ItemStack item = event.getItem();
    InventoryHolder holder = event.getHolder();
    
    // Seu código aqui
    // Ex: dar recompensa, efeitos, log, etc.
}
```

## API Pública

### TimedItemManager

```java
// Verificar se um item é temporizado
boolean isTimed = manager.isTimedItem(itemStack);

// Dar item temporizado para um jogador
ItemStack timedItem = manager.giveTimedItem(player, itemStack, 3600); // 1 hora
```

### ItemStatsTracker

```java
TimedItemManager manager = ItemStatsTracker.getInstance().getTimedItemManager();
if (manager != null) {
    // Sistema está ativo
}
```

## Performance

O sistema foi otimizado para não causar lag:

- **Update Task**: Executa apenas para jogadores online e chunks carregados
- **Container Scan**: Executa em thread assíncrona com intervalo configurável
- **Cache em memória**: IDs de itens temporizados são cacheados para acesso rápido
- **Verificações otimizadas**: Apenas itens com tag `timed_tag` são processados

### Recomendações

- **Servidores grandes**: Aumente `container_scan_interval_seconds` para 120 ou mais
- **Servidores pequenos**: Mantenha valores padrão ou até reduza para atualizações mais frequentes
- **MySQL**: Use apenas se precisar de backup externo ou queries complexas

## Limitações

- Itens temporizados não podem ser combinados com itens normais (o PDC pode se perder)
- Items em containers não carregados não são atualizados até serem abertos
- O banco de dados é principalmente para auditoria - os dados principais estão no PDC

## Suporte

Para problemas ou sugestões, abra uma issue no repositório do plugin.

---

**Versão:** 1.0.0  
**Plugin:** ItemStatsTracker  
**API:** Paper 1.21.4

