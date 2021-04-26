///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.concurrent.Callable;

@Command(name = "compose", mixinStandardHelpOptions = true, version = "compose 0.1",
        description = "Build a docker compose script with multiple WildFly versions")
class compose implements Callable<Integer> {

    @Parameters(arity = "1..*", description = "at least one WildFly major version number")
    private int[] versions;

    public static void main(String... args) {
        int exitCode = new CommandLine(new compose()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        FileWriter file = new FileWriter("docker-compose.yml");
        PrintWriter writer = new PrintWriter(file);
        writer.printf("version: \"3.9\"%n");
        writer.printf("services:%n");

        // redis
        writer.printf("  mgt-redis:%n");
        writer.printf("    image: \"redis\"%n");
        writer.printf("    ports:%n");
        writer.printf("      - \"6379:6379\"%n");

        // api service
        writer.printf("  mgt-api:%n");
        writer.printf("    image: \"modelgraphtools/api\"%n");
        writer.printf("    depends_on:%n");
        writer.printf("      - \"mgt-redis\"%n");
        writer.printf("    ports:%n");
        writer.printf("      - \"9911:8080\"%n");
        writer.printf("    environment:%n");
        writer.printf("      - quarkus.redis.hosts=redis://mgt-redis:6379%n");

        // neo4j
        for (int version : versions) {
            writer.printf("  mgt-neo4j-%d:%n", version);
            writer.printf("    image: \"modelgraphtools/neo4j:%d.0.0.Final\"%n", version);
            writer.printf("    ports:%n");
            writer.printf("      - \"74%d:7474\"%n", version);
            writer.printf("      - \"76%d:7687\"%n", version);
        }

        // model service
        for (int version : versions) {
            writer.printf("  mgt-model-%d:%n", version);
            writer.printf("    image: \"modelgraphtools/model\"%n");
            writer.printf("    depends_on:%n");
            writer.printf("      - \"mgt-api\"%n", version);
            writer.printf("      - \"mgt-neo4j-%d\"%n", version);
            writer.printf("    environment:%n");
            writer.printf("      - quarkus.neo4j.uri=bolt://mgt-neo4j-%d:7687%n", version);
            writer.printf("      - mgt.api.service.uri=http://mgt-api:8080%n");
            writer.printf("      - mgt.model.service.uri=http://mgt-model-%d:8080%n", version);
            writer.printf("      - mgt.neo4j.browser.uri=http://localhost:74%d%n", version);
        }

        // browser
        writer.printf("  mgt-browser:%n");
        writer.printf("    image: \"modelgraphtools/browser\"%n");
        writer.printf("    depends_on:%n");
        writer.printf("      - \"mgt-api\"%n");
        writer.printf("    ports:%n");
        writer.printf("      - \"80:80\"%n");
        writer.printf("    environment:%n");
        writer.printf("      - MGT_API=mgt-api:8080%n");

        writer.close();
        return 0;
    }
}
