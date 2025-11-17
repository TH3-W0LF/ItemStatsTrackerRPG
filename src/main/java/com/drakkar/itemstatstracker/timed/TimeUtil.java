package com.drakkar.itemstatstracker.timed;

/**
 * Utilitário para formatação de tempo
 */
public class TimeUtil {
    
    /**
     * Formata o tempo restante até a expiração em formato legível
     * @param expireAt timestamp de expiração em milissegundos
     * @return String formatada (ex: "1d 2h 30m 45s")
     */
    public static String formatRemaining(long expireAt) {
        long ms = expireAt - System.currentTimeMillis();
        
        if (ms <= 0) {
            return "0s";
        }
        
        long s = ms / 1000;
        long days = s / 86400;
        s %= 86400;
        long hours = s / 3600;
        s %= 3600;
        long minutes = s / 60;
        s %= 60;
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(s).append("s");
        
        return sb.toString().trim();
    }
}

