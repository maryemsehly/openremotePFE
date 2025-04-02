package org.openremote.agent.protocol.modbus;

import org.openremote.model.syslog.SyslogCategory;
import java.util.logging.Logger;

import static org.openremote.model.syslog.SyslogCategory.PROTOCOL;

/**
 * Classe de base pour les protocoles Modbus.
 * Cette classe sert de point d'entrée pour les fonctionnalités communes à tous les protocoles Modbus.
 */
public class ModbusProtocol {

    protected static final Logger LOG = SyslogCategory.getLogger(PROTOCOL, ModbusProtocol.class);

    // Constantes pour les codes de fonction Modbus
    public static final int READ_COILS = 1;
    public static final int READ_DISCRETE_INPUTS = 2;
    public static final int READ_HOLDING_REGISTERS = 3;
    public static final int READ_INPUT_REGISTERS = 4;
    public static final int WRITE_SINGLE_COIL = 5;
    public static final int WRITE_SINGLE_REGISTER = 6;
    public static final int WRITE_MULTIPLE_COILS = 15;
    public static final int WRITE_MULTIPLE_REGISTERS = 16;

    // Constantes pour les délais et tentatives
    public static final int DEFAULT_TIMEOUT_MS = 5000;
    public static final int DEFAULT_RETRY_COUNT = 3;
    public static final int DEFAULT_RETRY_DELAY_MS = 1000;

    /**
     * Vérifie si un code de fonction Modbus est valide.
     *
     * @param functionCode Le code de fonction à vérifier
     * @return true si le code de fonction est valide, false sinon
     */
    public static boolean isValidFunctionCode(int functionCode) {
        return switch (functionCode) {
            case READ_COILS, READ_DISCRETE_INPUTS, READ_HOLDING_REGISTERS, READ_INPUT_REGISTERS,
                 WRITE_SINGLE_COIL, WRITE_SINGLE_REGISTER, WRITE_MULTIPLE_COILS, WRITE_MULTIPLE_REGISTERS -> true;
            default -> false;
        };
    }

    /**
     * Convertit un code de fonction Modbus en chaîne de caractères descriptive.
     *
     * @param functionCode Le code de fonction à convertir
     * @return Une chaîne de caractères décrivant le code de fonction
     */
    public static String functionCodeToString(int functionCode) {
        return switch (functionCode) {
            case READ_COILS -> "Lecture de bobines (FC01)";
            case READ_DISCRETE_INPUTS -> "Lecture d'entrées discrètes (FC02)";
            case READ_HOLDING_REGISTERS -> "Lecture de registres de maintien (FC03)";
            case READ_INPUT_REGISTERS -> "Lecture de registres d'entrée (FC04)";
            case WRITE_SINGLE_COIL -> "Écriture d'une bobine (FC05)";
            case WRITE_SINGLE_REGISTER -> "Écriture d'un registre (FC06)";
            case WRITE_MULTIPLE_COILS -> "Écriture de plusieurs bobines (FC15)";
            case WRITE_MULTIPLE_REGISTERS -> "Écriture de plusieurs registres (FC16)";
            default -> "Code de fonction inconnu: " + functionCode;
        };
    }
}

