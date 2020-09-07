package org.javaee8.servlet.http2;

import java.io.File;
import java.net.URL;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;


/**
 * Test for the HTTP/2 protocol
 */
@RunWith(Arquillian.class)
public class Http2Test {

    @ArquillianResource
    private URL url;

    private HttpClient client;

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive war = create(WebArchive.class).addClasses(Servlet.class)
                .addAsWebResource(new File("src/main/webapp/images/payara-logo.jpg"), "images/payara-logo.jpg")
                .addAsWebInfResource(new File("src/main/webapp/WEB-INF/web.xml"))
                .addAsResource("project-defaults.yml"); // only for Thorntail
        System.out.println("War file content: \n" + war.toString(true));
        return war;
    }

    @Before
    public void setup() throws Exception {
        HttpClientTransportOverHTTP2 http2Transport = new HttpClientTransportOverHTTP2(new HTTP2Client());
        SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
//        final String[] excludeCipherSuites = sslContextFactory.getExcludeCipherSuites();
//        System.out.println("XX default excludedCipherSuite: " + Arrays.toString(excludeCipherSuites));
////        final List<String> newExcludeCS = Arrays.stream(excludeCipherSuites).filter(s -> !s.equals("^SSL_.*$")).collect(Collectors.toList());
//        final List<String> newExcludeCS = Arrays.asList("NOTHING", "HERE");
//        System.out.println("XX newExcludeCS: " + newExcludeCS);
//        sslContextFactory.setExcludeCipherSuites(Arrays.toString(newExcludeCS.toArray()));
////        System.out.println("XX selectedCipherSuites: " + Arrays.toString(sslContextFactory.getSelectedCipherSuites()));
////        System.out.println("XX selectedProtocols: " + Arrays.toString(sslContextFactory.getSelectedProtocols()));
        client = new HttpClient(http2Transport, sslContextFactory);
        client.start();
    }

    @After
    public void cleanUp() throws Exception {
        client.stop();
    }


    /**
     * This test runs against the public website supporting HTTP/2
     *
     * @throws Exception
     */
    @Test(timeout = 10000L)
    @RunAsClient
//    @Ignore
    public void testHttp2ControlGroup() throws Exception {
        System.out.println("java.version: " + System.getProperty("java.version")
                                   + "\n" + System.getProperty("environment")
                                   + "\n" + System.getProperty("mvn.version")
                                   + "\n" + System.getProperty("maven.version"));
        ContentResponse response = client.GET("https://http2.akamai.com/demo");
        System.out.println("RESPONSE=" + response);
        assertEquals("HTTP/2 should be used", HttpVersion.HTTP_2, response.getVersion());
    }

    //    https://www.cloudflare.com/
    @Test(timeout = 10000L)
    @RunAsClient
    public void testHttp2ControlGroup2() throws Exception {
        ContentResponse response = client.GET("https://www.cloudflare.com/");
        System.out.println("RESPONSE=" + response);
        assertEquals("HTTP/2 should be used", HttpVersion.HTTP_2, response.getVersion());
    }

    /**
     * This test runs against our private website supporting HTTP/2
     *
     * @throws Exception
     */
    @Test(timeout = 10000L)
    @RunAsClient
    public void testServerHttp2() throws Exception {
        System.out.println("Running testServerHttp2()");
        ContentResponse response = client.GET(url.toURI());
        // the header 'protocol' is set in the Servlet class.
        assertEquals(
                "Request wasn't over HTTP/2. Either the wrong servlet was returned, or the server doesn't support HTTP/2.",
                "HTTP/2", response.getHeaders().get("protocol"));
    }
}
