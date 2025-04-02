package org.openremote.agent.protocol.modbus;

import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;
import java.util.logging.Level;

public class ModbusTcpProtocol extends AbstractModbusProtocol<ModbusTcpProtocol, ModbusTcpAgent> {

    public ModbusTcpProtocol(ModbusTcpAgent agent) {
        super(agent);
    }

    @Override
    protected PlcConnection createIoClient(ModbusTcpAgent agent) throws RuntimeException {
        PlcConnection plcConnection = null;
        try {
            // Récupération des paramètres de connexion
            String host = agent.getHost().orElseThrow(() ->
                    new RuntimeException("Hôte non spécifié pour l'agent Modbus TCP"));

            Integer port = agent.getPort().orElseThrow(() ->
                    new RuntimeException("Port non spécifié pour l'agent Modbus TCP"));

            LOG.info("Connexion au serveur Modbus TCP à " + host + ":" + port);

            // Création de la chaîne de connexion avec un format différent
            // Ajout de l'ID d'unité dans la chaîne de connexion
            Integer unitId = agent.getUnitId().orElse(1);
            String connectionString = String.format("modbus-tcp://%s:%d/%d", host, port, unitId);

            // Utiliser PlcDriverManager.getDefault() au lieu de new PlcDriverManager()
            PlcDriverManager driverManager = PlcDriverManager.getDefault();

            // Utiliser la méthode correcte pour obtenir une connexion
            plcConnection = driverManager.getConnectionManager().getConnection(connectionString);

            if (!plcConnection.isConnected()) {
                plcConnection.connect();
            }

            LOG.info("Connexion réussie au serveur Modbus TCP à " + host + ":" + port);

        } catch (PlcConnectionException e) {
            LOG.log(Level.SEVERE, "Échec de connexion au serveur Modbus TCP", e);
            throw new RuntimeException("Échec de connexion au serveur Modbus TCP: " + e.getMessage(), e);
        }
        return plcConnection;
    }
}

