///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.6.1

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.concurrent.Callable;

@Command(name = "openshift", mixinStandardHelpOptions = true, version = "openshift 0.1",
        description = "Build a OpenShift setup script with multiple WildFly versions")
class openshift implements Callable<Integer> {

    private final static String APPLICATION_LABEL = "modelgraphtools";
    private final static String API_VERSION = "0.0.1";
    private final static String BROWSER_VERSION = "0.0.1";
    private final static String MODEL_VERSION = "0.0.1";

    @Parameters(arity = "1..*", description = "at least one WildFly major version number")
    private int[] versions;

    public static void main(String... args) {
        int exitCode = new CommandLine(new openshift()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        File file = new File("oc-setup.sh");
        FileWriter fw = new FileWriter(file);
        PrintWriter writer = new PrintWriter(fw);

        // redis
        writer.printf("oc new-app --template=redis-ephemeral \\%n");
        writer.printf("  --name=mgt-redis \\%n");
        writer.printf("  --labels application=%s \\%n", APPLICATION_LABEL);
        writer.printf("  --param REDIS_PASSWORD=redis \\%n");
        writer.printf("  --param DATABASE_SERVICE_NAME=mgt-redis%n");
        writer.printf("%n");

        // api service
        writer.printf("oc new-app quay.io/modelgraphtools/api:%s \\%n", API_VERSION);
        writer.printf("  --name=mgt-api \\%n");
        writer.printf("  --labels application=%s \\%n", APPLICATION_LABEL);
        writer.printf("  --env quarkus.redis.hosts=redis://:redis@mgt-redis:6379%n");
        writer.printf("oc expose service/mgt-api%n");
        writer.printf("%n");

        // browser
        writer.printf("oc new-app quay.io/modelgraphtools/browser:%s \\%n", BROWSER_VERSION);
        writer.printf("  --name=mgt-browser \\%n");
        writer.printf("  --labels application=%s \\%n", APPLICATION_LABEL);
        writer.printf("  --env MGT_API=mgt-api:8080%n");
        writer.printf("oc expose service/mgt-browser%n");
        writer.printf("BROWSER_ENDPOINT=$(oc get -o template route/mgt-browser --template={{.spec.host}})%n");
        writer.printf("%n");

        // neo4j
        for (int version : versions) {
            writer.printf("oc new-app quay.io/modelgraphtools/neo4j:%d.0.0.Final \\%n", version);
            writer.printf("  --name=mgt-neo4j-%d \\%n", version);
            writer.printf("  --labels application=%s \\%n", APPLICATION_LABEL);
            writer.printf("  --env NEO4J_browser_post__connect__cmd=\"play http://${BROWSER_ENDPOINT}/model-graph-guide.html\" \\%n");
            writer.printf("  --env NEO4J_browser_remote__content__hostname__whitelist=\"*\"%n");
            writer.printf("%n");
        }

        // model service
        for (int version : versions) {
            writer.printf("oc new-app quay.io/modelgraphtools/model:%s \\%n", MODEL_VERSION);
            writer.printf("  --name=mgt-model-%d \\%n", version);
            writer.printf("  --labels application=%s \\%n", APPLICATION_LABEL);
            writer.printf("  --env quarkus.neo4j.uri=bolt://mgt-neo4j-%d:7687 \\%n", version);
            writer.printf("  --env mgt.api.service.uri=http://mgt-api:8080 \\%n");
            writer.printf("  --env mgt.model.service.uri=http://mgt-model-%d:8080%n", version);
            writer.printf("%n");
        }

        writer.close();
        file.setExecutable(true);
        return 0;
    }
}
