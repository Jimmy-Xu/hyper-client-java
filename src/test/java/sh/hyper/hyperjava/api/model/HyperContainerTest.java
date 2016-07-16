package sh.hyper.hyperjava.api.model;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.jaxrs.DockerCmdExecFactoryImpl;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Jimmy Xu(xjimmyshcn@gmail.com)
 */
public class HyperContainerTest {

    @Test
    public void testCreateContainer() throws IOException {

        DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://us-west-1.hyper.sh:443")
                .withApiVersion("1.23")
                .withDockerTlsVerify(true)
                .withDockerCertPath(System.getProperty("user.home") + "/.hyper/certs")
                .build();

        // using jaxrs/jersey implementation here (netty impl is also available)
        DockerCmdExecFactory dockerCmdExecFactory = new DockerCmdExecFactoryImpl()
                .withReadTimeout(1000)
                .withConnectTimeout(1000)
                .withMaxTotalConnections(100)
                .withMaxPerRouteConnections(10);

        DockerClient dockerClient = DockerClientBuilder.getInstance(config)
                .withDockerCmdExecFactory(dockerCmdExecFactory)
                .build();

        CreateContainerResponse container = dockerClient.createContainerCmd("busybox")
                .withCmd("touch", "/test")
                .exec();
        assertThat(container, notNullValue());
        System.out.printf("CreateContainerResponse id:%s\n", container.getId());
    }

}
