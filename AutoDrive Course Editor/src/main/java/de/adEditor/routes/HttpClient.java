package de.adEditor.routes;

import com.google.gson.Gson;
import de.adEditor.routes.dto.Group;
import de.adEditor.routes.dto.Marker;
import de.adEditor.routes.dto.RouteExport;
import de.adEditor.routes.dto.Waypoints;
import de.autoDrive.NetworkServer.rest.dto_v1.GroupDto;
import de.autoDrive.NetworkServer.rest.dto_v1.MarkerDto;
import de.autoDrive.NetworkServer.rest.dto_v1.RoutesRequestDto;
import de.autoDrive.NetworkServer.rest.dto_v1.WaypointDto;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequests;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.Timeout;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;


public class HttpClient {

    public void request() throws URISyntaxException, ExecutionException, InterruptedException {
        IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                .setSoTimeout(Timeout.ofSeconds(5))
                .build();

        CloseableHttpAsyncClient client = HttpAsyncClients.custom()
                .setIOReactorConfig(ioReactorConfig)
                .build();

        client.start();

        HttpHost target = new HttpHost("localhost", 8080);
        String[] requestUris = new String[]{"/", "/ip", "/user-agent", "/headers"};

        for (String requestUri : requestUris) {
            SimpleHttpRequest httpget = SimpleHttpRequests.get(target, requestUri);
            System.out.println("Executing request " + httpget.getMethod() + " " + httpget.getUri());
            Future<SimpleHttpResponse> future = client.execute(
                    httpget,
                    new FutureCallback<SimpleHttpResponse>() {

                        @Override
                        public void completed(final SimpleHttpResponse response) {
                            System.out.println(requestUri + "->" + response.getCode());
                            System.out.println(response.getBody());
                        }

                        @Override
                        public void failed(final Exception ex) {
                            System.out.println(requestUri + "->" + ex);
                        }

                        @Override
                        public void cancelled() {
                            System.out.println(requestUri + " cancelled");
                        }

                    });
            future.get();
        }

        System.out.println("Shutting down");
        client.close(CloseMode.GRACEFUL);
    }

    public void upload(RouteExport routeExport, String name, String map, Integer revision, String date) throws ExecutionException, InterruptedException {

        RoutesRequestDto dto = toDto(routeExport,  name,  map,  revision,  date);

        IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                .setSoTimeout(Timeout.ofSeconds(5))
                .build();

        CloseableHttpAsyncClient client = HttpAsyncClients.custom()
                .setIOReactorConfig(ioReactorConfig)
                .build();

        client.start();

        HttpHost target = new HttpHost("localhost", 8080);
        SimpleHttpRequest httppost = SimpleHttpRequests.post(target, "/autodrive");
        Gson gson = new Gson();
        String body = gson.toJson(dto);
        httppost.setBody(body, ContentType.APPLICATION_JSON);
        Future<SimpleHttpResponse> future = client.execute(  httppost,
                new FutureCallback<SimpleHttpResponse>() {

                    @Override
                    public void completed(final SimpleHttpResponse response) {
                    }

                    @Override
                    public void failed(final Exception ex) {
                    }

                    @Override
                    public void cancelled() {
                    }

                });
        future.get();
    }

    private RoutesRequestDto toDto(RouteExport routeExport, String name, String map, Integer revision, String date) {
        RoutesRequestDto dto = new RoutesRequestDto();
        dto.setDate(date);
        dto.setName(name);
        dto.setRevision(revision);
        dto.setGroups(routeExport.getGroups().stream().map(group -> toGroupDto(group)).collect(Collectors.toList()));
        dto.setMarkers(routeExport.getMarkers().stream().map(m -> toMarkerDto(m)).collect(Collectors.toList()));
        dto.setWaypoints(toWaypointDtos (routeExport.getWaypoints()));
        return dto;
    }

    private List<WaypointDto> toWaypointDtos(Waypoints waypoints) {

        // TODO: remove stream
        List<WaypointDto> dtos = Arrays.stream(waypoints.getX().split(";")).map(x -> {
            WaypointDto wp = new WaypointDto();
            wp.setX(Double.valueOf(x));
            return wp;
        }).collect(Collectors.toList());

        String[] y = waypoints.getY().split(";");
        String[] z = waypoints.getZ().split(";");
        String[] out = waypoints.getOut().split(";");

        for (int i=0; i<dtos.size(); i++)
        {
            WaypointDto dto = dtos.get(i);
            dto.setY(Double.valueOf(y[i]));
            dto.setZ(Double.valueOf(z[i]));
            String[] oValue = out[i].split(",");
            for (String s : oValue) {
                dto.getOut().add(Integer.valueOf(s));
            }
        }

        return dtos;
    }

    private MarkerDto toMarkerDto(Marker marker) {
        var dto = new MarkerDto();
        dto.setGroup(marker.getGroup());
        dto.setName(marker.getName());
        dto.setWaypointIndex(marker.getWaypointIndex());
        return dto;
    }

    private GroupDto toGroupDto(Group group) {
        var dto = new GroupDto();
        dto.setName(group.getName());
        return dto;
    }


}
