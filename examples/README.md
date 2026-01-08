# SWIFT Connector Example Mule Application

This example demonstrates how to use the SWIFT Connector.

## Configuration

1. Update `src/main/resources/config.properties` with your SWIFT credentials
2. Deploy to Mule Runtime 4.10+

## Endpoints

- `POST /send-payment` - Send MT103 payment
- `GET /track/{uetr}` - Track gpi payment
- `GET /validate` - Validate message schema

## Running

```bash
mvn clean package
mule -M-Dfile.encoding=UTF-8 target/swift-example-1.0.0-SNAPSHOT.jar
```

