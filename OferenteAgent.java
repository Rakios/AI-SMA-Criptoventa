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
import java.util.concurrent.ThreadLocalRandom;

public class OferenteAgent extends Agent {
	// Lista de las monedas que el oferante esta interesado en comprar
	private Hashtable monedaInteres;
	


	// Inicializacion del agente
	protected void setup() {
		// Crear la lista de las monedas que esta interesado en comprar
		monedaInteres = new Hashtable();



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

	

	// se recibe las ofertas de venta de monedas 
	private class OfertaCoin extends CyclicBehaviour {
		public void action() {
			
			// Recibir ofertas de venta de moneda
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) { // si el mensaje no es nulo la oferta es valida
				
				// Procesar el contenido de la oferta 
				String contenido = msg.getContent(); // obtener el contenido del mensaje
				String[] cont = contenido.split(" ");
				
				String coin = cont[0]; // obtener nombre de la moneda
				String priceOriginal = cont[1]; // obtener el valor que esta interesado en vender el intermediario
				
				String cantidad =  cont[2]; // obtener la cantidad
				String monedaFid = cont[3]; // obtener la moneda fidusaria
				String metodoPago = cont[4]; // obtener el metodo de pago
				
				//Generar un regateo del precio de forma random
				int min = -10;
                int max = 10;
               // int regateo = (int)Math.random() * (max - min + 1) + min; 
                int regateo = ThreadLocalRandom.current().nextInt(min, max + 1);
				int price= Integer.parseInt(priceOriginal) + regateo;
	
				
				//Generar un regateo del precio de forma random
				//double minCant = 0.01;
               // double maxCant =  Double.parseDouble(cantidad) ;
               // int regateo = (int)Math.random() * (max - min + 1) + min; 
               // double cantBuy = ThreadLocalRandom.current().nextInt(minCant, maxCant + 1);
				
				//Generar random para saber si el oferente esta activo o no
				 min = 0;
                 max = 1;
                int activo = ThreadLocalRandom.current().nextInt(min, max + 1);
				
				
				
				
				ACLMessage reply = msg.createReply(); // crear un mensaje de respuesta
		//		Integer price = (Integer) monedaInteres.get(coin); // obtener el valor que esta interesado en pagar el oferente por la moneda
				
				if (price != 0 && activo == 1 ) { // si el valor no es nulo
				
					// Realizar una proposicion de oferta por la moneda
				
					System.out.println("Oferente: "+getAID().getName()+" quiere comprar "+cantidad+" unidades de "+coin +" y ofrece: "+price+" en "+metodoPago);
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(price));
				}
				else {
					// No estas interesado en comprar esa moneda, por lo que rechazas la peticion
					System.out.println("Oferente: "+getAID().getName()+" No esta interesado" );
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
				
				
				// se crea un mensaje para responder a la propocision
				
				
				// se obtiene el precio y se elimina la moneda de la lista de intereses
			//	Integer price = (Integer) monedaInteres.remove(coin);
				
				String contenido = msg.getContent(); // obtener el contenido del mensaje
				String[] cont = contenido.split(" ");
				
				String coin = cont[0]; // obtener nombre de la moneda
				String priceOriginal = cont[1]; // obtener el valor que esta interesado en vender el intermediario
				
				ACLMessage reply = msg.createReply();
				
				if (priceOriginal != null) {
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
		//myGui.dispose();
		// Mensaje de despedida
		System.out.println("Oferente-agent "+getAID().getName()+" terminating.");
	}
	
}



