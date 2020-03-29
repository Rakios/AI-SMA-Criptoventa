package cryptoAgent;


import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

public class OferenteAgent extends Agent {
	// Lista de las monedas que el oferante esta interesado en comprar
	private Hashtable monedaInteres;
	
	// GUI para agregar nuevas monedas a la lista
	private OferenteGui myGui;

	// Inicializacion del agente
	protected void setup() {
		// Crear la lista de las monedas que esta interesado en comprar
		monedaInteres = new Hashtable();

		// Generar la GUI
		myGui = new OferenteGui(this);
		myGui.showGui();

		// Registrar el agente en el directorio
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		
		// Indicar que es un agente de tipo compra de criptomnedas
		sd.setType("crypto-compra");
		sd.setName("JADE-crypto-trading");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		// Agregar metodo para las peticiones de ventas del intermediario
		addBehaviour(new OfertaCoin());

		// Agregar metodo para responder a la comprar de monedas
		addBehaviour(new CompraCoin());
	}

	
    // Gui para actualizar la lista de monedas del oferente
	public void updateMonedaInteres(final String coin, final int price) {
		addBehaviour(new OneShotBehaviour() {
			public void action() {
				monedaInteres.put(coin, new Integer(price));
				System.out.println("Interesado en comprar "+coin+" al precio = "+price);
			}
		} );
	}

	// se recibe las ofertas de venta de monedas 
	private class OfertaCoin extends CyclicBehaviour {
		public void action() {
			
			// Recibir ofertas de venta de moneda
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) { // si el mensaje no es nulo la oferta es valida
				
				// Procesar el contenido de la oferta 
				String coin = msg.getContent(); // obtener el nombre de la moneda
				
				ACLMessage reply = msg.createReply(); // crear un mensaje de respuesta
				Integer price = (Integer) monedaInteres.get(coin); // obtener el valor que esta interesado en pagar el oferente por la moneda
				
				if (price != null) { // si el valor no es nulo, es que esta en la lista de monedas 
				
					// Realizar una proposicion de oferta por la moneda
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(price.intValue()));
				}
				else {
					// No estas interesado en comprar esa moneda, por lo que rechazas la peticion
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("no-interesado");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  

	// Se obtiene la respuesta a la proposicion de compra enviada al intermediario
	private class CompraCoin extends CyclicBehaviour {
		public void action() {
			
			// Si se recibe un mensaje con la performativa de accept proposal
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			
			if (msg != null) { // si el mensaje no esta vacio es una aceptacion a la oferta
				
				// Se procesa el contenido del mensaje
				String coin = msg.getContent();
				// se crea un mensaje para responder a la propocision
				ACLMessage reply = msg.createReply();
				
				// se obtiene el precio y se elimina la moneda de la lista de intereses
				Integer price = (Integer) monedaInteres.remove(coin);
				
				if (price != null) {
					reply.setPerformative(ACLMessage.INFORM);
					System.out.println(coin+" comprada al  Intermediario "+msg.getSender().getName());
				}
				else {
					// Ya se a comprado la cantidad de monedas interesadas por el comprador
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("no-interesado");
				}
				
				// enviar respuesta
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfertaCoin

	// Eliminacion del agente
	protected void takeDown() {
		// quitarlo del directorio de agentes
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// cerrar la GUI
		myGui.dispose();
		// Mensaje de despedida
		System.out.println("Oferente-agent "+getAID().getName()+" terminating.");
	}
	
}



