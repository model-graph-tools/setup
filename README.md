# Model Graph Tools Setup

This repository contains documentation, configuration and scripts on how to get started with the model graph tools. The model graph tools are a set of tools and services to analyze and work with the WildFly management model:

<img src="https://model-graph-tools.github.io/img/tools.svg" alt="Model Graph Tools" width="512" />

The [analyzer](https://github.com/model-graph-tools/analyzer) creates a Neo4j database containing the management model of a given WildFly version. For ech major WildFly version starting with version 10.0.0.Final there are predefined Neo4j database images available at https://hub.docker.com/r/modelgraphtools/neo4j. 

The setup described here use these images. If you want to use another WildFly version or want to analyze a version of JBoss EAP, use the [analyzer](https://github.com/model-graph-tools/analyzer) command line tool to create your own Neo4j database. 

The [model](https://github.com/model-graph-tools/model) services use these Neo4j databases and expose a REST API. The [API](https://github.com/model-graph-tools/api) service keeps a registry of all running model services and provides a unified access to the [browser](https://github.com/model-graph-tools/browser) - a SPA with an UI to browse, query and compare the WildFly management model.

Getting all services up and running requires some plumbing. There are scipts in the different repositories to make this as easy as possible.   

## Development

To run the model graph tools with two model services in development mode use a combination of the following scripts:

```shell
git clone https://github.com/model-graph-tools/api.git
git clone https://github.com/model-graph-tools/model.git
git clone https://github.com/model-graph-tools/browser.git

api/start-redis.sh
api/dev.sh
model/start-modeldb.sh 23
model/dev.sh 23
model/start-modeldb.sh 22
model/dev.sh 22
browser/dev.sh
```

The last command will open a browser at http://localhost:3000. 

## Docker Compose

To run in production mode, you can use a [jbang](https://www.jbang.dev/) script which creates a docker compose file. The script accepts a list of WildFly versions. The generated docker compose file takes care about all necessary configuration and starts all services. 

```shell
git clone https://github.com/model-graph-tools/setup.git

cd setup/compose
jbang compose.java 23 22
docker compose up -d
docker logs -fn 100 mgt_mgt-api_1
```

Use `docker logs -fn 100 mgt_mgt-api_1` to follow the log file of the API service and wait until you see log messages about the registration of the model services. Then open http://localhost.
