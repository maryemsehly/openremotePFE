package org.openremote.agent.protocol.modbus;

import com.digitalpetri.modbus.client.ModbusClient;
import com.digitalpetri.modbus.exceptions.ModbusExecutionException;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.ModbusTimeoutException;
import com.digitalpetri.modbus.pdu.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModbusLinkHelper {

    private static final Logger LOG = Logger.getLogger(ModbusLinkHelper.class.getName());

    /**
     * Retourne une Function qui, étant donné un {@link ModbusAgentLink},
     * appelle la fonction de lecture appropriée sur le {@link ModbusClient} fourni
     * et retourne le {@link ModbusResponsePdu} résultant.
     *
     * Exemple d'utilisation:
     * <pre>{@code
     *   Function<ModbusAgentLink, ModbusResponsePdu> readFunction =
     *       ModbusLinkHelper.createReadFunction(client, 1);
     *   ModbusResponsePdu responsePdu = readFunction.apply(agentLink);
     * }</pre>
     *
     * @param client Le {@link ModbusClient} utilisé pour effectuer les opérations
     * @param unitId L'identifiant de l'esclave/unité Modbus
     * @return Une Function qui prend un agentLink et retourne le {@link ModbusResponsePdu}.
     */
    public static Function<ModbusAgentLink, ModbusResponsePdu> createReadFunction(ModbusClient client, int unitId) {
        return (ModbusAgentLink agentLink) -> {
            try {
                ModbusAgentLink.ReadType readType = agentLink.getReadType();
                int readAddress = agentLink.getReadAddress();

                LOG.info("Lecture Modbus: type=" + readType + ", adresse=" + readAddress + ", unitId=" + unitId);

                return switch (readType) {
                    case COIL -> {
                        // Lecture d'une bobine à partir de readAddress
                        yield client.readCoils(
                                unitId,
                                new ReadCoilsRequest(readAddress, 1)
                        );
                    }
                    case DISCRETE -> {
                        // Lecture d'une entrée discrète à partir de readAddress
                        yield client.readDiscreteInputs(
                                unitId,
                                new ReadDiscreteInputsRequest(readAddress, 1)
                        );
                    }
                    case HOLDING -> {
                        // Lecture d'un registre de maintien à partir de readAddress
                        yield client.readHoldingRegisters(
                                unitId,
                                new ReadHoldingRegistersRequest(readAddress, 1)
                        );
                    }
                    case INPUT -> {
                        // Lecture d'un registre d'entrée à partir de readAddress
                        yield client.readInputRegisters(
                                unitId,
                                new ReadInputRegistersRequest(readAddress, 1)
                        );
                    }
                };
            } catch (ModbusExecutionException
                     | ModbusResponseException
                     | ModbusTimeoutException e) {
                // Encapsulation des exceptions vérifiées dans une exception d'exécution
                throw new RuntimeException("Erreur lors de la lecture des données Modbus: " + e.getMessage(), e);
            }
        };
    }

    /**
     * Retourne une Function qui, étant donné un {@link ModbusAgentLink},
     * appelle la fonction d'écriture appropriée sur le {@link ModbusClient} fourni
     * et retourne le {@link ModbusResponsePdu} résultant.
     *
     * Exemple d'utilisation:
     * <pre>{@code
     *   Function<ModbusAgentLink, ModbusResponsePdu> writeFunction =
     *       ModbusLinkHelper.createWriteFunction(client, 1, 1); // 1 => bobine ON ou valeur de registre
     *   ModbusResponsePdu responsePdu = writeFunction.apply(agentLink);
     * }</pre>
     *
     * @param client Le {@link ModbusClient} utilisé pour effectuer les opérations
     * @param unitId L'identifiant de l'esclave/unité Modbus
     * @param value  La valeur à écrire (peut être 0/1 pour les bobines ou un entier pour les registres)
     * @return Une Function qui prend un agentLink et retourne le {@link ModbusResponsePdu}.
     */
    public static Function<ModbusAgentLink, ModbusResponsePdu> createWriteFunction(ModbusClient client, int unitId, int value) {
        return (ModbusAgentLink agentLink) -> {
            try {
                ModbusAgentLink.WriteType writeType = agentLink.getWriteType();
                int writeAddress = agentLink.getWriteAddress();

                LOG.info("Écriture Modbus: type=" + writeType + ", adresse=" + writeAddress + ", valeur=" + value + ", unitId=" + unitId);

                return switch (writeType) {
                    case COIL -> {
                        // Écriture d'une bobine: 0 => OFF, non-zéro => ON
                        boolean coilValue = (value != 0);
                        yield client.writeSingleCoil(
                                unitId,
                                new WriteSingleCoilRequest(writeAddress, coilValue)
                        );
                    }
                    case HOLDING -> {
                        // Écriture d'un registre
                        yield client.writeSingleRegister(
                                unitId,
                                new WriteSingleRegisterRequest(writeAddress, value)
                        );
                    }
                };
            } catch (ModbusExecutionException
                     | ModbusResponseException
                     | ModbusTimeoutException e) {
                // Encapsulation des exceptions vérifiées dans une exception d'exécution
                throw new RuntimeException("Erreur lors de l'écriture des données Modbus: " + e.getMessage(), e);
            }
        };
    }

    /**
     * Exécute une fonction de lecture sur un client Modbus et retourne le résultat.
     *
     * @param client Le client Modbus
     * @param unitId L'identifiant de l'unité
     * @param agentLink Le lien d'agent Modbus
     * @return Le résultat de la lecture
     */
    public static Object executeRead(ModbusClient client, int unitId, ModbusAgentLink agentLink) {
        try {
            Function<ModbusAgentLink, ModbusResponsePdu> readFunction = createReadFunction(client, unitId);
            ModbusResponsePdu response = readFunction.apply(agentLink);
            return ModbusHelper.parseResponse(response);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erreur lors de l'exécution de la lecture Modbus", e);
            throw new RuntimeException("Erreur lors de l'exécution de la lecture Modbus", e);
        }
    }

    /**
     * Exécute une fonction d'écriture sur un client Modbus et retourne le résultat.
     *
     * @param client Le client Modbus
     * @param unitId L'identifiant de l'unité
     * @param agentLink Le lien d'agent Modbus
     * @param value La valeur à écrire
     * @return Le résultat de l'écriture
     */
    public static Object executeWrite(ModbusClient client, int unitId, ModbusAgentLink agentLink, int value) {
        try {
            Function<ModbusAgentLink, ModbusResponsePdu> writeFunction = createWriteFunction(client, unitId, value);
            ModbusResponsePdu response = writeFunction.apply(agentLink);
            return ModbusHelper.parseResponse(response);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erreur lors de l'exécution de l'écriture Modbus", e);
            throw new RuntimeException("Erreur lors de l'exécution de l'écriture Modbus", e);
        }
    }
}

