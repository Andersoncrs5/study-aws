package com.aws.app1.configs.apiGatewayV2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import software.amazon.awssdk.services.apigatewayv2.ApiGatewayV2Client;
import software.amazon.awssdk.services.apigatewayv2.model.*;

import java.util.Optional;

public class ApiGatewayInitializer implements CommandLineRunner {

    @Autowired
    private ApiGatewayV2Client apiGateway;

    @Override
    public void run(String... args) throws Exception {
        String apiId = createHttpApi();
        String integrationId = createIntegration(apiId);
        String routeId = createRoute(apiId, integrationId);
        String deployApi = deployApi(apiId);
    }

    private String createHttpApi() {
        final String apiName = "app1";
        
        GetApisResponse apisRequest = apiGateway.getApis(GetApisRequest.builder().build());

        Optional<Api> existingApi = apisRequest.items().stream()
                .filter(api -> api.name().equals(apiName))
                .findFirst();

        if (existingApi.isPresent()) {
            return existingApi.get().apiId();
        }

        CreateApiRequest request = CreateApiRequest.builder()
                .name(apiName)
                .protocolType(ProtocolType.HTTP)
                .target("http://localhost:8888/")
                .build();

        try {
            CreateApiResponse response = apiGateway.createApi(request);
            return response.apiId();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String createIntegration(String apiId) {
        final String targetUri = "http://localhost:8888/gateway/send";

        GetIntegrationsResponse integrationsResponse = apiGateway.getIntegrations(GetIntegrationsRequest.builder()
                .apiId(apiId)
                .build());

        Optional<Integration> existingIntegration = integrationsResponse.items().stream()
                .filter(i -> i.integrationUri() != null && i.integrationUri().equals(targetUri))
                .findFirst();

        if (existingIntegration.isPresent()) {
            return existingIntegration.get().integrationId();
        }

        CreateIntegrationRequest request = CreateIntegrationRequest.builder()
                .apiId(apiId)
                .integrationMethod("GET")
                .payloadFormatVersion("1.0")
                .integrationType(IntegrationType.HTTP_PROXY)
                .integrationUri(targetUri)
                .build();

        CreateIntegrationResponse integration = apiGateway.createIntegration(request);

        return integration.integrationId();
    }

    private String createRoute(String apiId, String integrationId) {
        GetRoutesResponse routeResponse = apiGateway.getRoutes(GetRoutesRequest.builder()
                        .apiId(apiId)
                        .build());

        Optional<Route> first = routeResponse.items().stream()
                .filter(r -> r.target().equals("integrations/" + integrationId) && r.routeKey().equals("GET /gateway/send"))
                .findFirst();

        if (first.isPresent()) {
            return first.get().routeId();
        }

        CreateRouteRequest request = CreateRouteRequest.builder()
                .apiId(apiId)
                .routeKey("GET /gateway/send")
                .target("integrations/" + integrationId)
                .build();

        CreateRouteResponse route = apiGateway.createRoute(request);

        return route.routeId();
    }

    private String deployApi(String apiId) {
        final String stageName = "dev";

        GetStagesResponse stagesResponse = apiGateway.getStages(GetStagesRequest.builder()
                .apiId(apiId)
                .build());

        Optional<Stage> first = stagesResponse.items().stream()
                .filter(d -> d.stageName().equals(stageName))
                .findFirst();

        if (first.isPresent()) {
            return first.get().stageName();
        }

        CreateDeploymentResponse deploymentResponse = apiGateway.createDeployment(CreateDeploymentRequest.builder()
                .apiId(apiId)
                .build());

        CreateStageRequest stageRequest = CreateStageRequest.builder()
                .apiId(apiId)
                .stageName(stageName)
                .deploymentId(deploymentResponse.deploymentId())
                .build();

        apiGateway.createStage(stageRequest);

        System.out.println("Stage '" + stageName + "' criado e vinculado ao deployment.");

        return stageName;

    }

}
