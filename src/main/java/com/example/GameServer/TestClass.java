package com.example.GameServer;

public class TestClass {
	public static void main(String[] args) {
		try {
			Class.forName("org.springframework.kafka.support.serializer.ProtobufDeserializer");
			System.out.println("Class found!");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
