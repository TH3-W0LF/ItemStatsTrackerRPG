package com.drakkar.itemstatstracker.timed;

import com.drakkar.itemstatstracker.ItemStatsTracker;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Gerenciador de banco de dados para itens temporizados
 * Suporta SQLite e MySQL
 */
public class TimedItemDBManager {
    
    private final ItemStatsTracker plugin;
    private Connection connection;
    
    public TimedItemDBManager(ItemStatsTracker plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Inicializa a conexão com o banco de dados
     */
    public void init() {
        FileConfiguration config = plugin.getConfig();
        String type = config.getString("timed-items.database.type", "SQLITE").toUpperCase();
        
        try {
            if ("MYSQL".equals(type)) {
                String host = config.getString("timed-items.database.mysql.host", "localhost");
                int port = config.getInt("timed-items.database.mysql.port", 3306);
                String database = config.getString("timed-items.database.mysql.database", "timeditems");
                String user = config.getString("timed-items.database.mysql.user", "root");
                String password = config.getString("timed-items.database.mysql.password", "password");
                
                String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false";
                connection = DriverManager.getConnection(url, user, password);
                
                plugin.getLogger().info("Conectado ao MySQL para itens temporizados.");
            } else {
                // SQLite
                String file = config.getString("timed-items.database.sqlite-file", "timed_items.db");
                Class.forName("org.sqlite.JDBC");
                String url = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/" + file;
                connection = DriverManager.getConnection(url);
                
                plugin.getLogger().info("Conectado ao SQLite para itens temporizados.");
            }
            
            createTableIfNotExists();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao conectar ao banco de dados de itens temporizados: " + e.getMessage(), e);
        }
    }
    
    /**
     * Cria a tabela se não existir
     */
    private void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS timed_items (" +
                "id VARCHAR(36) PRIMARY KEY," +
                "owner_uuid VARCHAR(36)," +
                "material VARCHAR(64)," +
                "amount INT," +
                "expire_at BIGINT," +
                "location TEXT," +
                "created_at BIGINT" +
                ")";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            plugin.getLogger().info("Tabela timed_items criada/verificada com sucesso.");
        }
    }
    
    /**
     * Insere um item temporizado no banco de dados
     */
    public void insertTimedItem(String id, UUID owner, String material, int amount, long expireAt, long createdAt) {
        String sql = "INSERT INTO timed_items(id, owner_uuid, material, amount, expire_at, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, owner == null ? null : owner.toString());
            ps.setString(3, material);
            ps.setInt(4, amount);
            ps.setLong(5, expireAt);
            ps.setLong(6, createdAt);
            
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao inserir item temporizado no banco: " + e.getMessage(), e);
        }
    }
    
    /**
     * Remove um item temporizado do banco de dados
     */
    public void deleteById(String id) {
        String sql = "DELETE FROM timed_items WHERE id = ?";
        
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Erro ao deletar item temporizado do banco: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fecha a conexão com o banco de dados
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Conexão com banco de dados de itens temporizados fechada.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Erro ao fechar conexão do banco de dados: " + e.getMessage(), e);
        }
    }
    
    /**
     * Verifica se a conexão está ativa
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}

