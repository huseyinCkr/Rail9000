package com.company;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URISyntaxException;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
public class Main {

    public static void main(String[] args)  throws URISyntaxException, Exception{

        Connection connection = null;
        try {
            // Producer
            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(
                    "tcp://localhost:61616");
            connection = connectionFactory.createConnection();
            Session session = connection.createSession(false,
                    Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue("Rail9000");
            MessageProducer producer = session.createProducer(queue);
            Document doc = parseXml();
            String xmlPayload = getXmlAsString(doc);
            Message msg = session.createTextMessage(xmlPayload);
            System.out.println("Sending xml '" + xmlPayload + "'");
            producer.send(msg);

            MessageConsumer consumer = session.createConsumer(queue);
            connection.start();
            TextMessage textMsg = (TextMessage) consumer.receive();
            String xml = textMsg.getText();
            System.out.println("Received: '" + xml + "'");
            Document receivedDoc = getXmlAsDOMDocument(xml);
            Node employeesNode = receivedDoc.getFirstChild();
            NodeList nodeList = employeesNode.getChildNodes();
            int empCount = 0;
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node childNode = nodeList.item(i);
                if (childNode.getNodeName().equals("ESChangeOfState")) {
                    empCount++;
                }
            }
            System.out.println("ESChangeOfState count: " + empCount);
            session.close();
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

    }

    private static Document parseXml() throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                .newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory
                .newDocumentBuilder();
        return documentBuilder.parse(Main.class
                .getResourceAsStream("emp.xml"));
    }

    public static String getXmlAsString(Document document) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        StringWriter writer = new StringWriter();
        transformer
                .transform(new DOMSource(document), new StreamResult(writer));
        String output = writer.getBuffer().toString().replaceAll("\n|\r", "");
        return output;
    }

    public static Document getXmlAsDOMDocument(String xmlString) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                .newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory
                .newDocumentBuilder();
        return documentBuilder.parse(
                new InputSource(new StringReader(xmlString)));
    }
}

