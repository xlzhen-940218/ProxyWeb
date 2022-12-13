package org.xlzhen.proxy;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.request.Method;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;

public class App extends NanoHTTPD {
    private Map<String, String> remoteIpToRequestHostMap;
    private HttpClient client;

    public App() throws IOException {
        super(60061);
        remoteIpToRequestHostMap = new HashMap<>();
        CookieHandler.setDefault(new CookieManager());
        client = HttpClient.newBuilder().cookieHandler(CookieHandler.getDefault())
                .followRedirects(HttpClient.Redirect.ALWAYS).build();
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("\nRunning! Point your browsers to http://localhost:60061/ \n");
    }

    public static void main(String[] args) {
        try {
            new App();
        } catch (IOException ioe) {
            System.err.println("Couldn't start server:\n" + ioe);
        }
    }

    @Override
    public Response handle(IHTTPSession session) {
        try {
            String host = "";
            System.out.printf("uri:%s\n", session.getUri());
            if(session.getParameters().containsKey("host_url")){
                remoteIpToRequestHostMap.put(session.getRemoteIpAddress(),session.getParameters().get("host_url").get(0));
            }
            host=remoteIpToRequestHostMap.get(session.getRemoteIpAddress());
            System.out.printf("host:%s\n", host);
            if(session.getParameters().containsKey("real_url")&&session.getParameters().get("real_url").get(0).length()>0){
                host = session.getParameters().get("real_url").get(0);
            }
            HttpRequest.Builder builder = null;
            if (session.getMethod() == Method.POST) {
                String url = String.format("%s%s", host, session.getUri());
                System.out.printf("POST URL:%s\n",url);
                builder = HttpRequest.newBuilder(URI.create(url));
                try {
                    Supplier<? extends InputStream> streamSupplier = (Supplier<InputStream>) () -> session.getInputStream();
                    builder.POST(HttpRequest.BodyPublishers.ofInputStream(streamSupplier));

                }catch (Exception ex){
                    ex.printStackTrace();
                }
            } else if (session.getMethod() == Method.GET) {
                //System.out.printf("request get:%s\n", session.getQueryParameterString());
                if(session.getQueryParameterString()!=null) {
                    String url = String.format("%s%s?%s", host, session.getUri(), session.getQueryParameterString());
                    System.out.printf("GET URL:%s\n",url);
                    builder = HttpRequest.newBuilder(URI.create(url));
                }else{
                    String url = String.format("%s%s", host, session.getUri());
                    System.out.printf("GET URL:%s\n",url);
                    builder = HttpRequest.newBuilder(URI.create(url));

                }builder.GET();

            }
            for (String key : session.getHeaders().keySet()) {
                if ("host".equalsIgnoreCase(key)
                        || "connection".equalsIgnoreCase(key)
                        || "content-length".equalsIgnoreCase(key)
                        || "upgrade".equalsIgnoreCase(key)) {
                    continue;
                }
                String value = session.getHeaders().get(key);
                //System.out.printf("request header:%s---%s\n", key, value);
                builder.setHeader(key, value);
            }
            HttpRequest request = builder.build();
            System.out.printf("url:%s\n", request.uri().toString());

            HttpResponse<InputStream> httpResponse = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            Response response = null;

            //byte[] data = httpResponse.body().readAllBytes();
            response = Response.newFixedLengthResponse(Status.OK, ""
                    , httpResponse.body().readAllBytes());

            for (String key : httpResponse.headers().map().keySet()) {
                response.addHeader(key, httpResponse.headers().firstValue(key).get());
                //System.out.printf("response header:%s---%s\n", key, response.getHeader(key));
            }
            //System.out.printf("response data:%s\n",new String(data, StandardCharsets.UTF_8));
            return response;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return Response.newFixedLengthResponse(e.getMessage());
        }

    }
}