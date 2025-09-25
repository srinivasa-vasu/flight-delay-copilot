# Flight delay predictions: An Agentic AI Application

It is a Spring Boot application that demonstrates the use of agentic AI to predict flight delays. It showcases how modern Java technologies, combined with a powerful distributed SQL database, can be used to build sophisticated, AI-driven applications.

## The Power of Java, Spring AI, and Embabel

This project is built on a foundation of Java, Spring AI, and Embabel, a combination that offers a unique set of advantages for building AI agents:

*   **Java:** As a mature, robust, and performant language, Java provides the stability and scalability required for enterprise-grade AI applications. Its strong typing and extensive ecosystem of libraries and tools make it an ideal choice for building complex, mission-critical systems.

*   **Spring AI:** Spring AI simplifies the development of AI applications in the Spring ecosystem. It provides a unified API for interacting with various AI models and services, allowing developers to focus on building business logic rather than dealing with the complexities of integrating with different AI providers.

*   **Embabel:** Embabel is a Java-native library that brings the power of agentic AI to the Java world. It is based on the **Goal-Oriented Action Planning (GOAP)** architecture, which allows agents to dynamically create a sequence of actions to achieve a goal. This is in contrast to more rigid systems like Finite State Machines, and it allows for more flexible and adaptive agent behavior.

By combining these three technologies, we can build a powerful and reliable AI agent that is easy to maintain, scale, and integrate with other enterprise systems.

## The `predictFlightDelay` Agentic Action

The heart of this application is the `predictFlightDelay` agentic action. When invoked, the AI agent performs a multi-step analysis to predict the likelihood of a flight delay. This process is orchestrated by the Embabel agent, which uses its GOAP-based planning capabilities to determine the best course of action. The agent's actions are defined in the [DelayPredictCoPilotAgent](src/main/java/io/ai/agent/DelayPredictCoPilot.java) file and include:

1.  **`extractRequest`:** Extracts the origin, destination, and travel date from the user's input.
2.  **`buildContext`:** Builds a Retrieval-Augmented Generation (RAG) context by searching for similar documents in the vector store.
3.  **`optimizeContext`:** Ranks the retrieved documents by relevance to the prediction request.
4.  **`weatherAnalysis`:** Gets the weather forecast for the origin and destination airports.
5.  **`predictFlightDelay`:** This is the main action that predicts the flight delay. It uses the other actions to gather information and then performs a multi-step analysis using different personas:
    *   **Initial analysis:** An "analyst" persona generates an initial prediction.
    *   **Critique:** A "skeptic" persona critiques the initial prediction.
    *   **Final prediction:** A "lead analyst" persona synthesizes the initial prediction and the critique to produce a final prediction.

## Why YugabyteDB?

A high-performance, distributed SQL database that is PostgreSQL-compatible. YugabyteDB offers several key advantages for this example project:

*   **Scalability and Resilience:** YugabyteDB is a distributed database, which means it can scale horizontally to handle large volumes of data and high-throughput workloads. Its distributed nature also provides high availability and resilience, ensuring that the application remains operational even in the event of a node failure.

*   **PostgreSQL Compatibility:** YugabyteDB is runtime compatible with PostgreSQL, which means we can use the same drivers, tools, and libraries that we would use with PostgreSQL. This makes it easy to integrate with the Spring Boot application and allows us to leverage the rich ecosystem of PostgreSQL tools and extensions.

*   **Support for Vector Embeddings:** YugabyteDB supports the `pgvector` extension, which allows us to store and query vector embeddings directly in the database. This is a critical feature for AI agents, as it enables agents to perform efficient similarity searches on the flight data, leading to more accurate predictions.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

* Java 21
* Maven
* YugabyteDB

### Installation

1.  **Clone the repository:**

    ```bash
    git clone https://github.com/srinivasa-vasu/flight-delay-copilot.git
    ```

2.  **Set up the database:**

    This project uses YugabyteDB. You can use Docker or yugabyted to run a local instance.
    You can find detailed instructions [here](https://docs.yugabyte.com/stable/reference/configuration/yugabyted/) for running YugabyteDB using yugabyted.

3.  **Configure the application:**

    Create an `application.yml` file in `src/main/resources` with the following content:

    ```yaml
    spring:
      datasource:
        url: jdbc:postgresql://localhost:5433/yugabyte
        username: yugabyte
        password: yugabyte
    GEMINI_API_KEY: <GEMINI_API_KEY>
    ```

    Replace `<GEMINI_API_KEY>` with your API key. You can generate an API key from [Google AI Studio](https://aistudio.google.com/app/apikey).

## Build and Run

1.  **Build the project:**

    ```bash
    mvn clean install
    ```

2.  **Run the application:**

    ```bash
    mvn spring-boot:run
    ```

    The application will be available at `http://localhost:8080`.
