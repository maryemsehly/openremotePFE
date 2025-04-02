package org.openremote.agent.protocol.modbus;

import com.digitalpetri.modbus.client.ModbusClient;
import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
import com.digitalpetri.modbus.pdu.*;

import static org.openremote.agent.protocol.modbus.ModbusAgentLink.WriteType;
import static org.openremote.agent.protocol.modbus.ModbusAgentLink.ReadType;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ModbusHelper {

    private static final Logger LOG = Logger.getLogger(ModbusHelper.class.getName());

    /**
     * Effectue une opération de lecture sur le {@link ModbusClient} fourni
     * et retourne le {@link ModbusResponsePdu} résultant.
     *
     * @param client Le {@link ModbusClient} utilisé pour effectuer les opérations
     * @param unitId L'identifiant de l'esclave/unité Modbus
     * @param readType Le type d'opération de lecture à effectuer
     * @param readAddress L'adresse à partir de laquelle lire
     * @return Le {@link ModbusResponsePdu} résultant de l'opération de lecture.
     * @throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException
     */
    public static ModbusResponsePdu executeReadOperation(ModbusClient client, int unitId, ReadType readType, int readAddress)
            throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException {
        LOG.info("Exécution d'une opération de lecture Modbus: type=" + readType + ", adresse=" + readAddress);

        return switch (readType) {
            case COIL -> client.readCoils(unitId, new ReadCoilsRequest(readAddress, 1));
            case DISCRETE -> client.readDiscreteInputs(unitId, new ReadDiscreteInputsRequest(readAddress, 1));
            case HOLDING -> client.readHoldingRegisters(unitId, new ReadHoldingRegistersRequest(readAddress, 1));
            case INPUT -> client.readInputRegisters(unitId, new ReadInputRegistersRequest(readAddress, 1));
            default -> throw new ModbusExecutionException("Type de lecture non supporté: " + readType);
        };
    }

    /**
     * Effectue une opération d'écriture sur le {@link ModbusClient} fourni
     * et retourne le {@link ModbusResponsePdu} résultant.
     *
     * @param client Le {@link ModbusClient} utilisé pour effectuer les opérations
     * @param unitId L'identifiant de l'esclave/unité Modbus
     * @param writeType Le type d'opération d'écriture à effectuer
     * @param writeAddress L'adresse à laquelle écrire
     * @param value La valeur à écrire
     * @return Le {@link ModbusResponsePdu} résultant de l'opération d'écriture.
     * @throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException
     */
    public static ModbusResponsePdu executeWriteOperation(ModbusClient client, int unitId, WriteType writeType, int writeAddress, int value)
            throws ModbusExecutionException, ModbusResponseException, ModbusTimeoutException {
        LOG.info("Exécution d'une opération d'écriture Modbus: type=" + writeType + ", adresse=" + writeAddress + ", valeur=" + value);

        return switch (writeType) {
            case COIL -> {
                boolean coilValue = (value != 0);
                yield client.writeSingleCoil(unitId, new WriteSingleCoilRequest(writeAddress, coilValue));
            }
            case HOLDING -> {
                yield client.writeSingleRegister(unitId, new WriteSingleRegisterRequest(writeAddress, value));
            }
            default -> throw new ModbusExecutionException("Type d'écriture non supporté: " + writeType);
        };
    }

    /**
     * Analyse une réponse Modbus et extrait la valeur appropriée.
     *
     * @param response La réponse Modbus à analyser
     * @return La valeur extraite de la réponse
     * @throws ModbusExecutionException Si le type de réponse n'est pas reconnu
     */
    public static Object parseResponse(ModbusResponsePdu response) throws ModbusExecutionException {
        try {
            return switch (response) {
                case ReadCoilsResponse r -> r.coils();
                case ReadDiscreteInputsResponse r -> r.inputs();
                case ReadHoldingRegistersResponse r -> r.registers();
                case ReadInputRegistersResponse r -> r.registers();
                case WriteSingleCoilResponse r -> r.value();
                case WriteSingleRegisterResponse r -> r.value();
                case WriteMultipleCoilsResponse r -> r.quantity();
                case WriteMultipleRegistersResponse r -> r.quantity();
                case MaskWriteRegisterResponse r -> r.orMask();
                case ReadWriteMultipleRegistersResponse r -> r.registers();
                default -> throw new ModbusExecutionException("Type de réponse inattendu: " + response.getClass().getName());
            };
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erreur lors de l'analyse de la réponse Modbus", e);
            throw new ModbusExecutionException("Erreur lors de l'analyse de la réponse: " + e.getMessage());
        }
    }

    /**
     * Convertit une valeur en un type spécifique en fonction du type de valeur de lecture.
     *
     * @param value La valeur à convertir
     * @param valueType Le type de valeur cible
     * @return La valeur convertie
     */
    public static Object convertReadValue(Object value, ModbusAgentLink.ReadValueType valueType) {
        if (value == null) {
            return null;
        }

        try {
            return switch (valueType) {
                case INT16, INT32, INT64, INT8 -> convertToInteger(value);
                case UINT16, UINT32, UINT64, UINT8 -> convertToUnsignedInteger(value);
                case FLOAT32 -> convertToFloat(value);
                case BIT -> convertToBoolean(value);
                default -> value;
            };
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Erreur lors de la conversion de la valeur: " + e.getMessage(), e);
            return value;
        }
    }

    private static Integer convertToInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? 1 : 0;
        } else {
            return Integer.parseInt(value.toString());
        }
    }

    private static Long convertToUnsignedInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue() & 0xFFFFFFFFL; // Masque pour traiter comme non signé
        } else {
            return Long.parseLong(value.toString()) & 0xFFFFFFFFL;
        }
    }

    private static Float convertToFloat(Object value) {
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        } else {
            return Float.parseFloat(value.toString());
        }
    }

    private static Boolean convertToBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        } else {
            return Boolean.parseBoolean(value.toString());
        }
    }
}

