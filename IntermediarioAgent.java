package cryptoAgent;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class IntermediarioAgent extends Agent {
	// El nombre de la moneda que se quiere vender
	private String nombreCoin;
	private String price;
	private String cantidad;
	private String metodoPago;
	private String monedaFid;
	private AID cliente;
	// Lista de los oferentes activos
	private AID[] oferenteAgents;

	// Inicializacion del agente
	protected void setup() {
		
		// Mensaje de inicio
		System.out.println("Bienvenido a CryptoMarket_SMA ");
		System.out.println("El agente "+getAID().getName()+" esta listo para atender su solicitud");

		// Obtener el nombre de la moneda del argumento de creacion
		//Object[] args = getArguments();
	//	if (args != null && args.length > 0) {
	//		nombreCoin = (String) args[0];
	//		price = (String) args[1];
			
			// Se espera por el mensaje del cliente informando su venta
			System.out.println("Agent "+getLocalName()+": esperando por anuncio de venta...");
		ACLMessage msg = blockingReceive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
		if(msg != null){
		cliente = msg.getSender();
		String contenido = msg.getContent(); // obtener el contenido del mensaje
				String[] cont = contenido.split(" ");
				
				
				cantidad = (String) cont[1]; // obtener la cantidad
				nombreCoin = (String) cont[2]; // obtener nombre de la moneda
				price = (String) cont[4]; // obtener el valor que esta interesado en vender el intermediario
				monedaFid = (String) cont[5]; // obtener la moneda fidusaria
				metodoPago = (String) cont[7]; // obtener el metodo de pago
				
				if (nombreCoin != null ) {
			System.out.println("La criptomoneda que se quiere vender es: "+nombreCoin);
            System.out.println("Cantidad: "+cantidad+", precio: "+price+", moneda fidusaria: "+monedaFid+", metodo de pago: "+metodoPago);
			
			
			// Añadir un TickerBehaviour que repite una peticion a los Oferentes cada 30 seg
			addBehaviour(new TickerBehaviour(this, 30000) {
				protected void onTick() {
					System.out.println("Intentado vender "+nombreCoin);
					
					// Se inicializa un template para buscar a los agentes en el directorio
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					
					// que esten relacionados con el tipo crypto-compra
					sd.setType("crypto-compra");
					template.addServices(sd);
					try {
						// Se buscan los agentes basado en el template creado
						DFAgentDescription[] result = DFService.search(myAgent, template); 
						System.out.println("Se encontraron los siguientes oferentes:");
						oferenteAgents = new AID[result.length];
					
					// se almacenan los nombres de los Oferentes en un array
						for (int i = 0; i < result.length; ++i) {
							oferenteAgents[i] = result[i].getName();
							//System.out.println(oferenteAgents[i].getName());
						}
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}

					// Se realiza la peticion
					myAgent.addBehaviour(new RequestPerformer());
				}
			} );
		}else {
			// Make the agent terminate
			System.out.println("No se encontro el nombre de la moneda");
			doDelete();
		
		}
		}else{
			System.out.println("Mensage erroneo");
		doDelete();
		}
		}

	// Funcion para terminar el agente
	protected void takeDown() {
		
		System.out.println("Intermediario-agent "+getAID().getName()+" terminating.");
	}

	// Para realizar y responder la peticion de venta de moneda
	private class RequestPerformer extends Behaviour {
		private AID bestOferente; // El oferante con la mejor propuesta 
		private int bestPrice;  //  El mejor precio del oferante
		private int repliesCnt = 0; // La cantidad de respuestas recibidas
		private MessageTemplate mt; // La plantilla para recibir las respuestas
		private int step = 0;  // Llevar el control de la fase de la operacion

		public void action() {
			switch (step) {
				
			case 0: // Enviar el mensaje de interesado en vender moneda
				
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				// añadir a toda los oferente a la lista de enviar
				for (int i = 0; i < oferenteAgents.length; ++i) {
					cfp.addReceiver(oferenteAgents[i]);
				} 
				//añadir el nombre de la moneda al contenido del mensaje y el precio
				cfp.setContent(nombreCoin+" "+price+" "+cantidad+" "+monedaFid+" "+metodoPago);
				//indicar el id de la conversacion
				cfp.setConversationId("crypto-trade");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp); // enviar mensaje
				
				// Preparar un template para las respuestas
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("crypto-trade"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
						
				step = 1; //ir a la fase 1
				break;
				
			case 1: // analizar todas las propuestas y quedarse con la mejor
			
				// Recibir todas las propuestas del template anterior
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) { // si el mensaje no esta vacio, continuar
					
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						
						// Esta es una oferta 
						int price = Integer.parseInt(reply.getContent());
						if (bestOferente == null || price > bestPrice) {
							// Esta es la mejor oferta presente 
							bestPrice = price;
							bestOferente = reply.getSender();
						}
					}
					repliesCnt++;
					if (repliesCnt >= oferenteAgents.length) { // Si el numero de respuestas es igual a la cantidad de oferentes es que ya hemos recibido todas las respuestas
						
						step = 2; // ir a la fase 2
					}
				}
				else {
					block();
				}
				break;
				
			case 2: // enviar la orden de venta al oferente con el mejor precio
				
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				order.addReceiver(bestOferente);
				order.setContent(nombreCoin+" "+bestPrice);
				order.setConversationId("crypto-trade");
				order.setReplyWith("order"+System.currentTimeMillis());
				myAgent.send(order);
				// Prepare the template to get the purchase order reply
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("crypto-trade"),
						MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				step = 3;
				break;
				
				
			case 3: // recibir la respuesta de la orden 
			
				reply = myAgent.receive(mt);
				if (reply != null) {
					// Recibida respuesta a la venta
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Venta realizada 
						System.out.println("Intermediario: "+nombreCoin+" Exitosamente vendida al oferente "+reply.getSender().getName());
						System.out.println("Price = "+bestPrice);
						
					}
					else {
						System.out.println("Ha ocurrido un fallo: el oferente ya ha comprado monedas a otro vendedor.");
						
					}

					step = 4;
				}
				else {
					block();
				}
				break;
				
			case 4: // Informar al cliente que su moneda se ha vendido exitosamente
				
				ACLMessage resultado = new ACLMessage(ACLMessage.INFORM);
				resultado.addReceiver(cliente);
				resultado.setContent("Su moneda ha sido vendida exitosamente al oferente:"+ bestOferente+"por un costo de "+bestPrice );
				myAgent.send(resultado);
				// Prepare the template to get the purchase order reply
				
				myAgent.doDelete();
				step = 5;
				break;
			}        
		}

		public boolean done() {
			if (step == 2 && bestOferente == null) {
				System.out.println("Ha ocurrido un fallo: NA¿adie esta interesado en comprar "+nombreCoin);
			}
			return ((step == 2 && bestOferente == null) || step == 5);
		}
	}  // End of inner class RequestPerformer
}
