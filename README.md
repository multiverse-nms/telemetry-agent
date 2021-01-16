<h1 align="center"> Multiverse Network Management System [Telemetry Agent] </h1>
<!-- p align="center">
  <img src="docs/images/logo.png" />
</p -->

## Overview 
This is the Telemetry Agent component of the [Multiverse project](https://github.com/multiverse-nms/multiverse-controller).

> Note: The telemetry service is not secured yet; i.e., agents are not authenticated to the controller and communications are not encrypted.

## Deployment Instructions

The agent is deployable as a Java .jar executable.
The following instructions have been tested on Ubuntu (16.04, 18.04, 20.04), Windows 10, and macOS Catalina.

> Note: The agent requires the Multiverse controller to be already running.

### Prerequisites

- Java 8 (openJDK 1.8)
- Maven (versions 3.3 to 3.6 should work fine)

### Build and Run
```bash
git clone https://github.com/multiverse-nms/telemetry-agent.git
cd telemetry-agent
mvn clean install
java -jar target/nms-telemetry-agent-fat.jar src/conf/conf.json
```

> Note: In this example, the agent is configured to run on the same host as the controller.
More information on configuring the agents will be available soon.

