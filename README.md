# Sherlock Mysteries

Sherlock Mysteries is murder investigation game for Google Assistant.

## Build and Setup

Sherlock Mysteries consist of two main parts: Dialogflow based agent for Natural Language Processing and Google Could App Engine based fulfillment.

### Webhook fulfillment

Fulfillment part of the game keeps data about cases, handles player sessions, provides some simple administrative control over the game and part of game HTML based UI.

Please refer to [docs/Install.md](./docs/Install.md) on how to set it up.

### Dialogflow Agent

Dialogflow agent handles NLU processing for Sherlock Mysteries and uses Google Cloud fulfillment to finish user requests. To setup your agent go through following steps:

1. Register on [Dialogflow.com](https://dialogflow.com) and create new agent.
1. Go into agent settings into "Export and Import" section and press "Restore from Zip" button.
1. Use [SherlockMysteries.zip](./dialogflow-agent/SherlockMysteries.zip) to initialize your agent.
1. Go into Fulfillment section of your agent and setup URL that will be used as webhook endpoint. Endpoint should end with /webhook, for example: https://you-cloud-project-id.appspot.com/webhook .
